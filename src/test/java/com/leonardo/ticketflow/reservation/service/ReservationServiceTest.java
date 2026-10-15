package com.leonardo.ticketflow.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.exception.InvalidReservationException;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationItemRepository reservationItemRepository;

	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		reservationService = new ReservationService(
				userRepository,
				eventRepository,
				seatRepository,
				reservationRepository,
				reservationItemRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldCreatePendingReservationAndReserveSeats() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Seat firstSeat = seat(100L, "A1", event, new BigDecimal("250.00"));
		Seat secondSeat = seat(101L, "A2", event, new BigDecimal("250.00"));
		CreateReservationRequest request = new CreateReservationRequest(
				10L,
				List.of(101L, 100L));
		when(userRepository.findByEmailIgnoreCase("leonardo@example.com"))
				.thenReturn(Optional.of(user));
		when(eventRepository.findByIdAndStatus(10L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(event));
		when(seatRepository.findAllByIdWithLock(List.of(100L, 101L)))
				.thenReturn(List.of(firstSeat, secondSeat));
		when(reservationRepository.save(org.mockito.ArgumentMatchers.any(Reservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(reservationItemRepository.saveAll(
				org.mockito.ArgumentMatchers.<ReservationItem>anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ReservationResponse response = reservationService.create(
				"leonardo@example.com",
				request);

		assertThat(firstSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
		assertThat(secondSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
		assertThat(response.totalAmount()).isEqualByComparingTo("500.00");
		assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(600));
		assertThat(response.items()).extracting(item -> item.seatCode())
				.containsExactly("A1", "A2");
		verify(seatRepository).findAllByIdWithLock(List.of(100L, 101L));
	}

	@Test
	void shouldRejectUnavailableSeat() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Seat seat = seat(100L, "A1", event, new BigDecimal("250.00"));
		seat.reserve();
		when(userRepository.findByEmailIgnoreCase("leonardo@example.com"))
				.thenReturn(Optional.of(user));
		when(eventRepository.findByIdAndStatus(10L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(event));
		when(seatRepository.findAllByIdWithLock(List.of(100L))).thenReturn(List.of(seat));

		assertThatThrownBy(() -> reservationService.create(
				"leonardo@example.com",
				new CreateReservationRequest(10L, List.of(100L))))
				.isInstanceOf(SeatUnavailableException.class)
				.hasMessage("Seat is not available: A1");

		verifyNoInteractions(reservationRepository, reservationItemRepository);
	}

	@Test
	void shouldRejectSeatFromAnotherEvent() {
		User user = user(1L, "leonardo@example.com");
		Event selectedEvent = event(10L);
		Event anotherEvent = event(11L);
		Seat seat = seat(100L, "A1", anotherEvent, new BigDecimal("250.00"));
		when(userRepository.findByEmailIgnoreCase("leonardo@example.com"))
				.thenReturn(Optional.of(user));
		when(eventRepository.findByIdAndStatus(10L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(selectedEvent));
		when(seatRepository.findAllByIdWithLock(List.of(100L))).thenReturn(List.of(seat));

		assertThatThrownBy(() -> reservationService.create(
				"leonardo@example.com",
				new CreateReservationRequest(10L, List.of(100L))))
				.isInstanceOf(InvalidReservationException.class)
				.hasMessage("All seats must belong to the selected event");
	}

	@Test
	void shouldRejectDuplicatedSeatIds() {
		CreateReservationRequest request = new CreateReservationRequest(
				10L,
				List.of(100L, 100L));

		assertThatThrownBy(() -> reservationService.create("leonardo@example.com", request))
				.isInstanceOf(InvalidReservationException.class)
				.hasMessage("Seat ids must not be duplicated");

		verifyNoInteractions(
				userRepository,
				eventRepository,
				seatRepository,
				reservationRepository,
				reservationItemRepository);
	}

	private User user(Long id, String email) {
		User user = new User("Leonardo", email, "encoded-password", Role.USER);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Event event(Long id) {
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		event.changeStatus(EventStatus.AVAILABLE);
		ReflectionTestUtils.setField(event, "id", id);
		return event;
	}

	private Seat seat(Long id, String code, Event event, BigDecimal price) {
		Sector sector = new Sector(event, "VIP", price);
		ReflectionTestUtils.setField(sector, "id", 20L);
		Seat seat = new Seat(sector, code);
		ReflectionTestUtils.setField(seat, "id", id);
		return seat;
	}
}
