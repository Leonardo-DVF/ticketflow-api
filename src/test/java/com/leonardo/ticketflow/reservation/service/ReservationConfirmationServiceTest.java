package com.leonardo.ticketflow.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.TicketIssuedEvent;
import com.leonardo.ticketflow.kafka.producer.TicketEventProducer;
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
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.ticket.entity.Ticket;
import com.leonardo.ticketflow.ticket.repository.TicketRepository;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ReservationConfirmationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-16T15:00:00Z");

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationItemRepository reservationItemRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private RedisSeatLockService seatLockService;

	@Mock
	private TicketEventProducer ticketEventProducer;

	private ReservationConfirmationService confirmationService;

	@BeforeEach
	void setUp() {
		confirmationService = new ReservationConfirmationService(
				reservationRepository,
				reservationItemRepository,
				paymentRepository,
				seatRepository,
				ticketRepository,
				seatLockService,
				ticketEventProducer);
	}

	@Test
	void shouldConfirmReservationAndSellReservedSeat() {
		Fixture fixture = fixture();
		when(reservationRepository.findByIdWithLock(50L))
				.thenReturn(Optional.of(fixture.reservation()));
		when(paymentRepository.findByIdAndReservationIdAndStatus(
				80L,
				50L,
				PaymentStatus.APPROVED))
				.thenReturn(Optional.of(fixture.payment()));
		when(reservationItemRepository.findAllByReservationIdOrderById(50L))
				.thenReturn(List.of(fixture.item()));
		when(seatRepository.findAllByIdWithLock(List.of(100L)))
				.thenReturn(List.of(fixture.seat()));
		when(ticketRepository.saveAll(org.mockito.ArgumentMatchers.<Ticket>anyList()))
				.thenAnswer(invocation -> {
					List<Ticket> tickets = invocation.getArgument(0);
					ReflectionTestUtils.setField(tickets.getFirst(), "id", 90L);
					ReflectionTestUtils.setField(tickets.getFirst(), "createdAt", NOW);
					return tickets;
				});

		confirmationService.confirm(approvedEvent());

		assertThat(fixture.reservation().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(fixture.seat().getStatus()).isEqualTo(SeatStatus.SOLD);
		verify(seatLockService).release(10L, 100L, "reservation:50");
		ArgumentCaptor<TicketIssuedEvent> eventCaptor = ArgumentCaptor.forClass(
				TicketIssuedEvent.class);
		verify(ticketEventProducer).publish(eventCaptor.capture());
		assertThat(eventCaptor.getValue().ticketId()).isEqualTo(90L);
		assertThat(eventCaptor.getValue().reservationId()).isEqualTo(50L);
		assertThat(eventCaptor.getValue().seatId()).isEqualTo(100L);
		assertThat(eventCaptor.getValue().code()).isNotNull();
		assertThat(eventCaptor.getValue().issuedAt()).isEqualTo(NOW);
	}

	@Test
	void shouldIgnoreDuplicatedEventForConfirmedReservation() {
		Fixture fixture = fixture();
		fixture.reservation().confirm();
		when(reservationRepository.findByIdWithLock(50L))
				.thenReturn(Optional.of(fixture.reservation()));

		confirmationService.confirm(approvedEvent());

		verifyNoInteractions(
				paymentRepository,
				reservationItemRepository,
				seatRepository,
				ticketRepository,
				seatLockService,
				ticketEventProducer);
	}

	@Test
	void shouldRejectEventThatDoesNotMatchReservation() {
		Fixture fixture = fixture();
		when(reservationRepository.findByIdWithLock(50L))
				.thenReturn(Optional.of(fixture.reservation()));
		when(paymentRepository.findByIdAndReservationIdAndStatus(
				80L,
				50L,
				PaymentStatus.APPROVED))
				.thenReturn(Optional.of(fixture.payment()));
		PaymentApprovedEvent mismatchedEvent = new PaymentApprovedEvent(
				80L,
				50L,
				999L,
				10L,
				new BigDecimal("250.00"),
				NOW);

		assertThatThrownBy(() -> confirmationService.confirm(mismatchedEvent))
				.isInstanceOf(InvalidReservationException.class)
				.hasMessage("Payment event does not match the reservation");

		assertThat(fixture.reservation().getStatus()).isEqualTo(ReservationStatus.PENDING);
		assertThat(fixture.seat().getStatus()).isEqualTo(SeatStatus.RESERVED);
		verifyNoInteractions(
				reservationItemRepository,
				seatRepository,
				ticketRepository,
				seatLockService,
				ticketEventProducer);
	}

	private PaymentApprovedEvent approvedEvent() {
		return new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				NOW);
	}

	private Fixture fixture() {
		User user = new User("Leonardo", "leonardo@example.com", "password", Role.USER);
		ReflectionTestUtils.setField(user, "id", 1L);
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		ReflectionTestUtils.setField(event, "id", 10L);
		Sector sector = new Sector(event, "VIP", new BigDecimal("250.00"));
		Seat seat = new Seat(sector, "A1");
		ReflectionTestUtils.setField(seat, "id", 100L);
		seat.reserve();
		Reservation reservation = new Reservation(
				user,
				event,
				NOW.plusSeconds(600),
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", 50L);
		Payment payment = new Payment(reservation, new BigDecimal("250.00"));
		ReflectionTestUtils.setField(payment, "id", 80L);
		payment.approve();
		ReservationItem item = new ReservationItem(
				reservation,
				seat,
				new BigDecimal("250.00"));
		return new Fixture(reservation, seat, payment, item);
	}

	private record Fixture(
			Reservation reservation,
			Seat seat,
			Payment payment,
			ReservationItem item) {
	}
}
