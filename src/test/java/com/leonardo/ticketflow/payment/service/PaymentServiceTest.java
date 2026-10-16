package com.leonardo.ticketflow.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.leonardo.ticketflow.event.entity.Event;
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
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentEventProducer paymentEventProducer;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(
				reservationRepository,
				paymentRepository,
				paymentEventProducer,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldApprovePaymentForPendingReservation() {
		Reservation reservation = reservation();
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(paymentRepository.existsByReservationIdAndStatus(50L, PaymentStatus.APPROVED))
				.thenReturn(false);
		when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
				.thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 80L));

		PaymentResponse response = paymentService.approve("leonardo@example.com", 50L);

		assertThat(response.id()).isEqualTo(80L);
		assertThat(response.reservationId()).isEqualTo(50L);
		assertThat(response.amount()).isEqualByComparingTo("250.00");
		assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(response.createdAt()).isEqualTo(NOW);
		org.mockito.Mockito.verify(paymentEventProducer).publish(
				org.mockito.ArgumentMatchers.argThat((PaymentApprovedEvent event) ->
						event.paymentId().equals(80L)
								&& event.reservationId().equals(50L)
								&& event.userId().equals(1L)
								&& event.eventId().equals(10L)
								&& event.amount().compareTo(new BigDecimal("250.00")) == 0
								&& event.occurredAt().equals(NOW)));
	}

	@Test
	void shouldRejectPaymentAndKeepReservationPendingForRetry() {
		Reservation reservation = reservation();
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(paymentRepository.existsByReservationIdAndStatus(50L, PaymentStatus.APPROVED))
				.thenReturn(false);
		when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
				.thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 80L));

		PaymentResponse response = paymentService.reject("leonardo@example.com", 50L);

		assertThat(response.status()).isEqualTo(PaymentStatus.REJECTED);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
		org.mockito.Mockito.verify(paymentEventProducer).publish(
				org.mockito.ArgumentMatchers.argThat((PaymentRejectedEvent event) ->
						event.paymentId().equals(80L)
								&& event.reservationId().equals(50L)));
	}

	@Test
	void shouldPublishOnlyAfterTransactionCommit() {
		Reservation reservation = reservation();
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(paymentRepository.existsByReservationIdAndStatus(50L, PaymentStatus.APPROVED))
				.thenReturn(false);
		when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
				.thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 80L));
		TransactionSynchronizationManager.initSynchronization();

		try {
			paymentService.approve("leonardo@example.com", 50L);

			verifyNoInteractions(paymentEventProducer);
			TransactionSynchronizationManager.getSynchronizations()
					.forEach(TransactionSynchronization::afterCommit);
			org.mockito.Mockito.verify(paymentEventProducer)
					.publish(org.mockito.ArgumentMatchers.any(PaymentApprovedEvent.class));
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldRejectExpiredReservation() {
		Reservation reservation = reservation();
		ReflectionTestUtils.setField(reservation, "expiresAt", NOW);
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> paymentService.approve("leonardo@example.com", 50L))
				.isInstanceOf(ReservationExpiredException.class)
				.hasMessage("Reservation has expired: 50");

		verifyNoInteractions(paymentRepository);
	}

	@Test
	void shouldRejectReservationThatIsNotPending() {
		Reservation reservation = reservation();
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> paymentService.approve("leonardo@example.com", 50L))
				.isInstanceOf(InvalidPaymentException.class)
				.hasMessage("Only pending reservations can be paid");

		verifyNoInteractions(paymentRepository);
	}

	@Test
	void shouldPreventAnotherDecisionAfterApprovedPayment() {
		Reservation reservation = reservation();
		when(reservationRepository.findOwnedByIdWithLock(50L, "leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(paymentRepository.existsByReservationIdAndStatus(50L, PaymentStatus.APPROVED))
				.thenReturn(true);

		assertThatThrownBy(() -> paymentService.reject("leonardo@example.com", 50L))
				.isInstanceOf(InvalidPaymentException.class)
				.hasMessage("Reservation already has an approved payment");
	}

	private Payment savedPayment(Payment payment, Long id) {
		ReflectionTestUtils.setField(payment, "id", id);
		ReflectionTestUtils.setField(payment, "createdAt", NOW);
		return payment;
	}

	private Reservation reservation() {
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"encoded-password",
				Role.USER);
		ReflectionTestUtils.setField(user, "id", 1L);
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		ReflectionTestUtils.setField(event, "id", 10L);
		Reservation reservation = new Reservation(
				user,
				event,
				NOW.plusSeconds(600),
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", 50L);
		return reservation;
	}
}
