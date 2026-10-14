package com.leonardo.ticketflow.seat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.leonardo.ticketflow.config.SecurityConfig;
import com.leonardo.ticketflow.exception.GlobalExceptionHandler;
import com.leonardo.ticketflow.seat.dto.CreateSeatRequest;
import com.leonardo.ticketflow.seat.dto.SeatResponse;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.exception.SeatAlreadyExistsException;
import com.leonardo.ticketflow.seat.service.SeatService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SeatController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class SeatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SeatService seatService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldAllowAdminToCreateSeat() throws Exception {
		CreateSeatRequest request = new CreateSeatRequest("A1");
		when(seatService.create(eq(10L), any(CreateSeatRequest.class)))
				.thenReturn(response());

		mockMvc.perform(post("/sectors/10/seats")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(100))
				.andExpect(jsonPath("$.code").value("A1"))
				.andExpect(jsonPath("$.status").value("AVAILABLE"))
				.andExpect(jsonPath("$.sectorName").value("VIP"))
				.andExpect(jsonPath("$.price").value(250.00));
	}

	@Test
	void shouldForbidRegularUserFromCreatingSeat() throws Exception {
		mockMvc.perform(post("/sectors/10/seats")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CreateSeatRequest("A1"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Access denied"));

		verifyNoInteractions(seatService);
	}

	@Test
	void shouldRejectInvalidSeatData() throws Exception {
		mockMvc.perform(post("/sectors/10/seats")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CreateSeatRequest(""))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(seatService);
	}

	@Test
	void shouldReturnConflictForDuplicatedSeat() throws Exception {
		when(seatService.create(eq(10L), any(CreateSeatRequest.class)))
				.thenThrow(new SeatAlreadyExistsException("A1"));

		mockMvc.perform(post("/sectors/10/seats")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CreateSeatRequest("A1"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Seat already exists in this sector: A1"));
	}

	@Test
	void shouldListEventSeatsWithoutAuthentication() throws Exception {
		when(seatService.findByEvent(1L)).thenReturn(List.of(response()));

		mockMvc.perform(get("/events/1/seats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("A1"))
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));
	}

	@Test
	void shouldListAvailableEventSeatsWithoutAuthentication() throws Exception {
		when(seatService.findAvailableByEvent(1L)).thenReturn(List.of(response()));

		mockMvc.perform(get("/events/1/seats/available"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("A1"))
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));
	}

	private SeatResponse response() {
		return new SeatResponse(
				100L,
				"A1",
				SeatStatus.AVAILABLE,
				10L,
				"VIP",
				new BigDecimal("250.00"));
	}
}
