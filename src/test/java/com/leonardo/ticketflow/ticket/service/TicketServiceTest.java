package com.leonardo.ticketflow.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.ticket.dto.TicketResponse;
import com.leonardo.ticketflow.ticket.entity.Ticket;
import com.leonardo.ticketflow.ticket.repository.TicketRepository;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-10-16T15:00:00Z");

	@Mock
	private TicketRepository ticketRepository;

	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(ticketRepository);
	}

	@Test
	void shouldListAuthenticatedUserTickets() {
		Ticket ticket = ticket();
		when(ticketRepository.findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(
				"leonardo@example.com"))
				.thenReturn(List.of(ticket));

		List<TicketResponse> response = ticketService.findMine("leonardo@example.com");

		assertThat(response).singleElement().satisfies(item -> {
			assertThat(item.id()).isEqualTo(90L);
			assertThat(item.reservationId()).isEqualTo(50L);
			assertThat(item.eventName()).isEqualTo("Coldplay - Brasilia");
			assertThat(item.eventLocation()).isEqualTo("Arena BRB Mane Garrincha");
			assertThat(item.seatCode()).isEqualTo("A1");
			assertThat(item.sectorName()).isEqualTo("VIP");
			assertThat(item.createdAt()).isEqualTo(CREATED_AT);
		});
	}

	@Test
	void shouldFindOwnedTicketById() {
		Ticket ticket = ticket();
		when(ticketRepository.findByIdAndUserEmailIgnoreCase(
				90L,
				"leonardo@example.com"))
				.thenReturn(Optional.of(ticket));

		TicketResponse response = ticketService.findById("leonardo@example.com", 90L);

		assertThat(response.id()).isEqualTo(90L);
		assertThat(response.code()).isEqualTo(ticket.getCode());
		assertThat(response.seatId()).isEqualTo(100L);
	}

	@Test
	void shouldHideTicketOwnedByAnotherUser() {
		when(ticketRepository.findByIdAndUserEmailIgnoreCase(
				90L,
				"another@example.com"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> ticketService.findById("another@example.com", 90L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Ticket not found: 90");
	}

	private Ticket ticket() {
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
		Reservation reservation = new Reservation(
				user,
				event,
				CREATED_AT.plusSeconds(600),
				new BigDecimal("250.00"));
		ReflectionTestUtils.setField(reservation, "id", 50L);
		Ticket ticket = new Ticket(reservation, seat);
		ReflectionTestUtils.setField(ticket, "id", 90L);
		ReflectionTestUtils.setField(ticket, "createdAt", CREATED_AT);
		return ticket;
	}
}
