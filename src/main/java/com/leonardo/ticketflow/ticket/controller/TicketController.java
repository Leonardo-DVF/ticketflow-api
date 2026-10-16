package com.leonardo.ticketflow.ticket.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.ticket.dto.TicketResponse;
import com.leonardo.ticketflow.ticket.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {

	private final TicketService ticketService;

	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping("/my")
	public List<TicketResponse> findMine(@AuthenticationPrincipal Jwt jwt) {
		return ticketService.findMine(jwt.getSubject());
	}

	@GetMapping("/{id}")
	public TicketResponse findById(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id) {
		return ticketService.findById(jwt.getSubject(), id);
	}
}
