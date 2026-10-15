package com.leonardo.ticketflow.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
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
	private SeatRepository seatRepository;

	private ReservationExpirationService expirationService;

	@BeforeEach
	void setUp() {
		expirationService = new ReservationExpirationService(
				reservationRepository,
				reservationItemRepository,
				seatRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldExpireDueReservationAndReleaseItsSeat() {
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		Sector sector = new Sector(event, "VIP", new BigDecimal("250.00"));
		Seat seat = new Seat(sector, "A1");
		seat.reserve();
		ReflectionTestUtils.setField(seat, "id", 100L);
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
		ReservationItem item = new ReservationItem(
				reservation,
				seat,
				new BigDecimal("250.00"));
		when(reservationRepository.findAllDueWithLock(ReservationStatus.PENDING, NOW))
				.thenReturn(List.of(reservation));
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of(item));
		when(seatRepository.findAllByIdWithLock(List.of(100L)))
				.thenReturn(List.of(seat));

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isEqualTo(1);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
	}

	@Test
	void shouldDoNothingWhenThereAreNoDueReservations() {
		when(reservationRepository.findAllDueWithLock(ReservationStatus.PENDING, NOW))
				.thenReturn(List.of());

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isZero();
		verifyNoInteractions(reservationItemRepository, seatRepository);
	}
}
