package com.leonardo.ticketflow.payment.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.common.transaction.TransactionActions;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;
import com.leonardo.ticketflow.kafka.producer.PaymentEventProducer;
import com.leonardo.ticketflow.payment.dto.PaymentResponse;
import com.leonardo.ticketflow.payment.entity.Payment;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;
import com.leonardo.ticketflow.payment.exception.InvalidPaymentException;
import com.leonardo.ticketflow.payment.repository.PaymentRepository;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.ReservationExpiredException;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;

@Service
public class PaymentService {

	private final ReservationRepository reservationRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentEventProducer paymentEventProducer;
	private final Clock clock;

	public PaymentService(
			ReservationRepository reservationRepository,
			PaymentRepository paymentRepository,
			PaymentEventProducer paymentEventProducer,
			Clock clock) {
		this.reservationRepository = reservationRepository;
		this.paymentRepository = paymentRepository;
		this.paymentEventProducer = paymentEventProducer;
		this.clock = clock;
	}

	@Transactional
	public PaymentResponse approve(String userEmail, Long reservationId) {
		return process(userEmail, reservationId, PaymentStatus.APPROVED);
	}

	@Transactional
	public PaymentResponse reject(String userEmail, Long reservationId) {
		return process(userEmail, reservationId, PaymentStatus.REJECTED);
	}

	private PaymentResponse process(
			String userEmail,
			Long reservationId,
			PaymentStatus result) {
		Reservation reservation = reservationRepository
				.findOwnedByIdWithLock(reservationId, userEmail)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Reservation not found: " + reservationId));

		validate(reservation);
		Payment payment = new Payment(reservation, reservation.getTotalAmount());
		if (result == PaymentStatus.APPROVED) {
			payment.approve();
		} else {
			payment.reject();
		}

		Payment savedPayment = paymentRepository.save(payment);
		registerEventAfterCommit(savedPayment);
		return toResponse(savedPayment);
	}

	private void registerEventAfterCommit(Payment payment) {
		Reservation reservation = payment.getReservation();
		if (payment.getStatus() == PaymentStatus.APPROVED) {
			PaymentApprovedEvent event = new PaymentApprovedEvent(
					payment.getId(),
					reservation.getId(),
					reservation.getUser().getId(),
					reservation.getEvent().getId(),
					payment.getAmount(),
					payment.getCreatedAt());
			TransactionActions.afterCommit(() -> paymentEventProducer.publish(event));
			return;
		}

		PaymentRejectedEvent event = new PaymentRejectedEvent(
				payment.getId(),
				reservation.getId(),
				reservation.getUser().getId(),
				reservation.getEvent().getId(),
				payment.getAmount(),
				payment.getCreatedAt());
		TransactionActions.afterCommit(() -> paymentEventProducer.publish(event));
	}

	private void validate(Reservation reservation) {
		if (reservation.getStatus() != ReservationStatus.PENDING) {
			throw new InvalidPaymentException("Only pending reservations can be paid");
		}
		if (!reservation.getExpiresAt().isAfter(clock.instant())) {
			throw new ReservationExpiredException(
					"Reservation has expired: " + reservation.getId());
		}
		if (paymentRepository.existsByReservationIdAndStatus(
				reservation.getId(),
				PaymentStatus.APPROVED)) {
			throw new InvalidPaymentException(
					"Reservation already has an approved payment");
		}
	}

	private PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getReservation().getId(),
				payment.getAmount(),
				payment.getStatus(),
				payment.getCreatedAt());
	}
}
