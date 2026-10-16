package com.leonardo.ticketflow.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.common.transaction.TransactionActions;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.payment.entity.Payment;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;
import com.leonardo.ticketflow.payment.repository.PaymentRepository;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.InvalidReservationException;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;

@Service
public class ReservationConfirmationService {

	private final ReservationRepository reservationRepository;
	private final ReservationItemRepository reservationItemRepository;
	private final PaymentRepository paymentRepository;
	private final SeatRepository seatRepository;
	private final RedisSeatLockService seatLockService;

	public ReservationConfirmationService(
			ReservationRepository reservationRepository,
			ReservationItemRepository reservationItemRepository,
			PaymentRepository paymentRepository,
			SeatRepository seatRepository,
			RedisSeatLockService seatLockService) {
		this.reservationRepository = reservationRepository;
		this.reservationItemRepository = reservationItemRepository;
		this.paymentRepository = paymentRepository;
		this.seatRepository = seatRepository;
		this.seatLockService = seatLockService;
	}

	@Transactional
	public void confirm(PaymentApprovedEvent event) {
		Reservation reservation = reservationRepository
				.findByIdWithLock(event.reservationId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Reservation not found: " + event.reservationId()));
		if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
			return;
		}
		validatePendingReservation(event, reservation);

		List<Long> seatIds = reservationItemRepository
				.findAllByReservationIdOrderById(reservation.getId())
				.stream()
				.map(ReservationItem::getSeat)
				.map(Seat::getId)
				.sorted()
				.toList();
		if (seatIds.isEmpty()) {
			throw new InvalidReservationException("Reservation has no seats");
		}

		List<Seat> seats = seatRepository.findAllByIdWithLock(seatIds);
		if (seats.size() != seatIds.size()
				|| seats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.RESERVED)) {
			throw new InvalidReservationException(
					"All reservation seats must be reserved before confirmation");
		}

		seats.forEach(Seat::sell);
		reservation.confirm();
		Long eventId = reservation.getEvent().getId();
		String lockOwner = "reservation:" + reservation.getId();
		TransactionActions.afterCommit(() -> seatIds.forEach(
				seatId -> releaseLockQuietly(eventId, seatId, lockOwner)));
	}

	private void validatePendingReservation(
			PaymentApprovedEvent event,
			Reservation reservation) {
		if (reservation.getStatus() != ReservationStatus.PENDING) {
			throw new InvalidReservationException(
					"Only pending reservations can be confirmed");
		}
		Payment payment = paymentRepository.findByIdAndReservationIdAndStatus(
				event.paymentId(),
				event.reservationId(),
				PaymentStatus.APPROVED)
				.orElseThrow(() -> new InvalidReservationException(
						"Approved payment not found: " + event.paymentId()));
		if (!reservation.getUser().getId().equals(event.userId())
				|| !reservation.getEvent().getId().equals(event.eventId())
				|| payment.getAmount().compareTo(event.amount()) != 0) {
			throw new InvalidReservationException(
					"Payment event does not match the reservation");
		}
	}

	private void releaseLockQuietly(Long eventId, Long seatId, String owner) {
		try {
			seatLockService.release(eventId, seatId, owner);
		} catch (RuntimeException ignored) {
			// The reservation is already confirmed; an expired Redis lock is harmless.
		}
	}
}
