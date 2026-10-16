package com.leonardo.ticketflow.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.kafka.event.ReservationExpiredEvent;
import com.leonardo.ticketflow.kafka.producer.ReservationEventProducer;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;
import com.leonardo.ticketflow.payment.repository.PaymentRepository;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationItemRepository reservationItemRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private RedisSeatLockService seatLockService;

	@Mock
	private ReservationEventProducer reservationEventProducer;

	private ReservationExpirationService expirationService;

	@BeforeEach
	void setUp() {
		expirationService = new ReservationExpirationService(
				reservationRepository,
				reservationItemRepository,
				paymentRepository,
				seatRepository,
				seatLockService,
				reservationEventProducer,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldExpireDueReservationAndReleaseItsSeat() {
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		ReflectionTestUtils.setField(event, "id", 10L);
		Sector sector = new Sector(event, "VIP", new BigDecimal("250.00"));
		Seat seat = new Seat(sector, "A1");
		seat.reserve();
		ReflectionTestUtils.setField(seat, "id", 100L);
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"encoded-password",
				Role.USER);
		ReflectionTestUtils.setField(user, "id", 1L);
		Reservation reservation = new Reservation(
				user,
				event,
				NOW,
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", 50L);
		ReservationItem item = new ReservationItem(
				reservation,
				seat,
				new BigDecimal("250.00"));
		when(reservationRepository.findAllDueWithLock(
				ReservationStatus.PENDING,
				NOW,
				PaymentStatus.APPROVED))
				.thenReturn(List.of(reservation));
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of(item));
		when(seatRepository.findAllByIdWithLock(List.of(100L)))
				.thenReturn(List.of(seat));
		doThrow(new RuntimeException("Redis unavailable"))
				.when(seatLockService)
				.release(10L, 100L, "reservation:50");

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isEqualTo(1);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
		verify(seatLockService).release(10L, 100L, "reservation:50");
		verify(reservationEventProducer).publish(new ReservationExpiredEvent(
				50L,
				1L,
				10L,
				List.of(100L),
				NOW));
	}

	@Test
	void shouldDoNothingWhenThereAreNoDueReservations() {
		when(reservationRepository.findAllDueWithLock(
				ReservationStatus.PENDING,
				NOW,
				PaymentStatus.APPROVED))
				.thenReturn(List.of());

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isZero();
		verifyNoInteractions(
				paymentRepository,
				reservationItemRepository,
				seatRepository,
				seatLockService,
				reservationEventProducer);
	}

	@Test
	void shouldSkipReservationApprovedAfterExpirationQueryStarted() {
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"encoded-password",
				Role.USER);
		Reservation reservation = new Reservation(
				user,
				event,
				NOW,
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", 50L);
		when(reservationRepository.findAllDueWithLock(
				ReservationStatus.PENDING,
				NOW,
				PaymentStatus.APPROVED))
				.thenReturn(List.of(reservation));
		when(paymentRepository.existsByReservationIdAndStatus(
				50L,
				PaymentStatus.APPROVED))
				.thenReturn(true);

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isZero();
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
		verifyNoInteractions(
				reservationItemRepository,
				seatRepository,
				seatLockService,
				reservationEventProducer);
	}
}
