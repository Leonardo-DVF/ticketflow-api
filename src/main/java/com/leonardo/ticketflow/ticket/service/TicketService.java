package com.leonardo.ticketflow.ticket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.ticket.dto.TicketResponse;
import com.leonardo.ticketflow.ticket.entity.Ticket;
import com.leonardo.ticketflow.ticket.repository.TicketRepository;

@Service
public class TicketService {

	private final TicketRepository ticketRepository;

	public TicketService(TicketRepository ticketRepository) {
		this.ticketRepository = ticketRepository;
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> findMine(String userEmail) {
		return ticketRepository.findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public TicketResponse findById(String userEmail, Long ticketId) {
		return ticketRepository.findByIdAndUserEmailIgnoreCase(ticketId, userEmail)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Ticket not found: " + ticketId));
	}

	private TicketResponse toResponse(Ticket ticket) {
		return new TicketResponse(
				ticket.getId(),
				ticket.getCode(),
				ticket.getReservation().getId(),
				ticket.getEvent().getId(),
				ticket.getEvent().getName(),
				ticket.getEvent().getLocation(),
				ticket.getEvent().getEventDate(),
				ticket.getSeat().getId(),
				ticket.getSeat().getCode(),
				ticket.getSeat().getSector().getName(),
				ticket.getCreatedAt());
	}
}
