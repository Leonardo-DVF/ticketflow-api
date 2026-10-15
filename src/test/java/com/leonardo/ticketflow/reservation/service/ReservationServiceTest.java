package com.leonardo.ticketflow.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.InvalidReservationException;
import com.leonardo.ticketflow.reservation.exception.ReservationNotCancellableException;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
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

	@Mock
	private RedisSeatLockService seatLockService;

	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		reservationService = new ReservationService(
				userRepository,
				eventRepository,
				seatRepository,
				reservationRepository,
				reservationItemRepository,
				seatLockService,
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
		when(seatLockService.acquire(eq(10L), eq(100L), anyString())).thenReturn(true);
		when(seatLockService.acquire(eq(10L), eq(101L), anyString())).thenReturn(true);
		when(reservationRepository.save(org.mockito.ArgumentMatchers.any(Reservation.class)))
				.thenAnswer(invocation -> {
					Reservation reservation = invocation.getArgument(0);
					ReflectionTestUtils.setField(reservation, "id", 50L);
					return reservation;
				});
		when(reservationItemRepository.saveAll(
				org.mockito.ArgumentMatchers.<ReservationItem>anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(seatLockService.transfer(eq(10L), eq(100L), anyString(), eq("reservation:50")))
				.thenReturn(true);
		when(seatLockService.transfer(eq(10L), eq(101L), anyString(), eq("reservation:50")))
				.thenReturn(true);

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
		when(seatLockService.acquire(eq(10L), eq(100L), anyString())).thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(
				"leonardo@example.com",
				new CreateReservationRequest(10L, List.of(100L))))
				.isInstanceOf(SeatUnavailableException.class)
				.hasMessage("Seat is not available: A1");

		verifyNoInteractions(reservationRepository, reservationItemRepository);
		verify(seatLockService).release(eq(10L), eq(100L), anyString());
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
		when(seatLockService.acquire(eq(10L), eq(100L), anyString())).thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(
				"leonardo@example.com",
				new CreateReservationRequest(10L, List.of(100L))))
				.isInstanceOf(InvalidReservationException.class)
				.hasMessage("All seats must belong to the selected event");
		verify(seatLockService).release(eq(10L), eq(100L), anyString());
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
				reservationItemRepository,
				seatLockService);
	}

	@Test
	void shouldReleasePreviouslyAcquiredLocksWhenOneSeatIsAlreadyLocked() {
		CreateReservationRequest request = new CreateReservationRequest(
				10L,
				List.of(100L, 101L));
		when(seatLockService.acquire(eq(10L), eq(100L), anyString())).thenReturn(true);
		when(seatLockService.acquire(eq(10L), eq(101L), anyString())).thenReturn(false);

		assertThatThrownBy(() -> reservationService.create(
				"leonardo@example.com",
				request))
				.isInstanceOf(SeatUnavailableException.class)
				.hasMessage("Seat is currently reserved: 101");

		verify(seatLockService).release(eq(10L), eq(100L), anyString());
		verifyNoInteractions(
				userRepository,
				eventRepository,
				seatRepository,
				reservationRepository,
				reservationItemRepository);
	}

	@Test
	void shouldFindOwnedReservationById() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Seat seat = seat(100L, "A1", event, new BigDecimal("250.00"));
		Reservation reservation = reservation(50L, user, event);
		ReservationItem item = new ReservationItem(
				reservation,
				seat,
				new BigDecimal("250.00"));
		when(reservationRepository.findByIdAndUserEmailIgnoreCase(
				50L,
				"leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of(item));

		ReservationResponse response = reservationService.findById(
				"leonardo@example.com",
				50L);

		assertThat(response.id()).isEqualTo(50L);
		assertThat(response.items()).extracting(itemResponse -> itemResponse.seatCode())
				.containsExactly("A1");
	}

	@Test
	void shouldListOnlyAuthenticatedUserReservations() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Reservation newer = reservation(51L, user, event);
		Reservation older = reservation(50L, user, event);
		when(reservationRepository.findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(
				"leonardo@example.com"))
				.thenReturn(List.of(newer, older));
		when(reservationItemRepository.findAllByReservationIdOrderById(51L))
				.thenReturn(List.of());
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of());

		List<ReservationResponse> responses = reservationService.findMine(
				"leonardo@example.com");

		assertThat(responses).extracting(ReservationResponse::id)
				.containsExactly(51L, 50L);
	}

	@Test
	void shouldCancelPendingReservationAndReleaseSeats() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Seat seat = seat(100L, "A1", event, new BigDecimal("250.00"));
		seat.reserve();
		Reservation reservation = reservation(50L, user, event);
		ReservationItem item = new ReservationItem(
				reservation,
				seat,
				new BigDecimal("250.00"));
		when(reservationRepository.findOwnedByIdWithLock(
				50L,
				"leonardo@example.com"))
				.thenReturn(Optional.of(reservation));
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of(item));
		when(seatRepository.findAllByIdWithLock(List.of(100L)))
				.thenReturn(List.of(seat));
		doThrow(new RuntimeException("Redis unavailable"))
				.when(seatLockService)
				.release(10L, 100L, "reservation:50");

		reservationService.cancel("leonardo@example.com", 50L);

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
		verify(seatLockService).release(10L, 100L, "reservation:50");
	}

	@Test
	void shouldRejectCancellationWhenReservationIsNotPending() {
		User user = user(1L, "leonardo@example.com");
		Event event = event(10L);
		Reservation reservation = reservation(50L, user, event);
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
		when(reservationRepository.findOwnedByIdWithLock(
				50L,
				"leonardo@example.com"))
				.thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.cancel(
				"leonardo@example.com",
				50L))
				.isInstanceOf(ReservationNotCancellableException.class)
				.hasMessage("Only pending reservations can be cancelled");

		verifyNoInteractions(seatRepository);
	}

	private Reservation reservation(Long id, User user, Event event) {
		Reservation reservation = new Reservation(
				user,
				event,
				NOW.plusSeconds(600),
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", id);
		ReflectionTestUtils.setField(reservation, "createdAt", NOW);
		return reservation;
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
