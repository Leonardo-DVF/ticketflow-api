package com.leonardo.ticketflow.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findAllByReservationIdOrderById(Long reservationId);
}
