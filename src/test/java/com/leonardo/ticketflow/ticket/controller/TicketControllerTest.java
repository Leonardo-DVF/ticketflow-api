package com.leonardo.ticketflow.ticket.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.leonardo.ticketflow.config.SecurityConfig;
import com.leonardo.ticketflow.exception.GlobalExceptionHandler;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.ticket.dto.TicketResponse;
import com.leonardo.ticketflow.ticket.service.TicketService;

@WebMvcTest(TicketController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class TicketControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TicketService ticketService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldListAuthenticatedUserTickets() throws Exception {
		when(ticketService.findMine("leonardo@example.com"))
				.thenReturn(List.of(response()));

		mockMvc.perform(get("/tickets/my")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(90))
				.andExpect(jsonPath("$[0].eventName").value("Coldplay - Brasilia"))
				.andExpect(jsonPath("$[0].seatCode").value("A1"))
				.andExpect(jsonPath("$[0].sectorName").value("VIP"));
	}

	@Test
	void shouldFindAuthenticatedUserTicket() throws Exception {
		when(ticketService.findById("leonardo@example.com", 90L))
				.thenReturn(response());

		mockMvc.perform(get("/tickets/90")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(90))
				.andExpect(jsonPath("$.code")
						.value("d4203978-e77a-4f9b-b99c-3c42f865f54c"));
	}

	@Test
	void shouldReturnNotFoundForTicketNotOwnedByUser() throws Exception {
		when(ticketService.findById("another@example.com", 90L))
				.thenThrow(new ResourceNotFoundException("Ticket not found: 90"));

		mockMvc.perform(get("/tickets/90")
				.with(jwt().jwt(jwt -> jwt.subject("another@example.com"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Ticket not found: 90"));
	}

	@Test
	void shouldRequireAuthenticationToListTickets() throws Exception {
		mockMvc.perform(get("/tickets/my"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(ticketService);
	}

	private TicketResponse response() {
		return new TicketResponse(
				90L,
				UUID.fromString("d4203978-e77a-4f9b-b99c-3c42f865f54c"),
				50L,
				10L,
				"Coldplay - Brasilia",
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"),
				100L,
				"A1",
				"VIP",
				Instant.parse("2026-10-16T15:00:00Z"));
	}
}
