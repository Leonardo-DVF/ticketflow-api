package com.leonardo.ticketflow.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.leonardo.ticketflow.config.SecurityConfig;
import com.leonardo.ticketflow.exception.GlobalExceptionHandler;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationItemResponse;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.service.ReservationService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ReservationController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReservationService reservationService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldCreateReservationForAuthenticatedUser() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest(1L, List.of(100L));
		when(reservationService.create(
				eq("leonardo@example.com"),
				any(CreateReservationRequest.class)))
				.thenReturn(response());

		mockMvc.perform(post("/reservations")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(50))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.totalAmount").value(250.00))
				.andExpect(jsonPath("$.items[0].seatCode").value("A1"));
	}

	@Test
	void shouldRequireAuthenticationToCreateReservation() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest(1L, List.of(100L));

		mockMvc.perform(post("/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(reservationService);
	}

	@Test
	void shouldRejectInvalidReservationData() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest(null, List.of());

		mockMvc.perform(post("/reservations")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(reservationService);
	}

	@Test
	void shouldReturnConflictForUnavailableSeat() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest(1L, List.of(100L));
		when(reservationService.create(
				eq("leonardo@example.com"),
				any(CreateReservationRequest.class)))
				.thenThrow(new SeatUnavailableException("A1"));

		mockMvc.perform(post("/reservations")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Seat is not available: A1"));
	}

	private ReservationResponse response() {
		return new ReservationResponse(
				50L,
				1L,
				ReservationStatus.PENDING,
				Instant.parse("2026-10-15T15:10:00Z"),
				new BigDecimal("250.00"),
				Instant.parse("2026-10-15T15:00:00Z"),
				List.of(new ReservationItemResponse(
						100L,
						"A1",
						"VIP",
						new BigDecimal("250.00"))));
	}
}
