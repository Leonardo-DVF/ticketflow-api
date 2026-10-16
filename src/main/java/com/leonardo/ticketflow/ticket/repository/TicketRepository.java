package com.leonardo.ticketflow.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findAllByReservationIdOrderById(Long reservationId);

	@EntityGraph(attributePaths = { "reservation", "event", "seat", "seat.sector" })
	List<Ticket> findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

	@EntityGraph(attributePaths = { "reservation", "event", "seat", "seat.sector" })
	Optional<Ticket> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
