package com.leonardo.ticketflow.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
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
import com.leonardo.ticketflow.event.dto.CreateEventRequest;
import com.leonardo.ticketflow.event.dto.EventResponse;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.service.EventService;
import com.leonardo.ticketflow.exception.GlobalExceptionHandler;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(EventController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldAllowAdminToCreateEvent() throws Exception {
		CreateEventRequest request = validRequest();
		EventResponse response = new EventResponse(
				1L,
				"Coldplay - Brasilia",
				"Music of the Spheres Tour",
				"Arena BRB Mane Garrincha",
				request.eventDate(),
				EventStatus.DRAFT,
				Instant.parse("2026-10-09T18:00:00Z"));
		when(eventService.create(any(CreateEventRequest.class))).thenReturn(response);

		mockMvc.perform(post("/events")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Coldplay - Brasilia"))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.eventDate").value("2026-12-20T20:00:00-03:00"));
	}

	@Test
	void shouldForbidRegularUserFromCreatingEvent() throws Exception {
		mockMvc.perform(post("/events")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("Access denied"))
				.andExpect(jsonPath("$.path").value("/events"));

		verifyNoInteractions(eventService);
	}

	@Test
	void shouldRequireAuthenticationToCreateEvent() throws Exception {
		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication is required"));

		verifyNoInteractions(eventService);
	}

	@Test
	void shouldRejectInvalidEventData() throws Exception {
		CreateEventRequest request = new CreateEventRequest(
				"",
				null,
				"",
				OffsetDateTime.parse("2020-01-01T20:00:00-03:00"));

		mockMvc.perform(post("/events")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/events"));

		verifyNoInteractions(eventService);
	}

	@Test
	void shouldListAvailableEventsWithoutAuthentication() throws Exception {
		EventResponse response = eventResponse();
		when(eventService.findAvailable()).thenReturn(List.of(response));

		mockMvc.perform(get("/events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Coldplay - Brasilia"))
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));
	}

	@Test
	void shouldFindAvailableEventByIdWithoutAuthentication() throws Exception {
		when(eventService.findAvailableById(1L)).thenReturn(eventResponse());

		mockMvc.perform(get("/events/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Coldplay - Brasilia"))
				.andExpect(jsonPath("$.status").value("AVAILABLE"));
	}

	@Test
	void shouldReturnNotFoundForUnavailableOrUnknownEvent() throws Exception {
		when(eventService.findAvailableById(99L))
				.thenThrow(new ResourceNotFoundException("Event not found: 99"));

		mockMvc.perform(get("/events/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Event not found: 99"))
				.andExpect(jsonPath("$.path").value("/events/99"));
	}

	private CreateEventRequest validRequest() {
		return new CreateEventRequest(
				"Coldplay - Brasilia",
				"Music of the Spheres Tour",
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
	}

	private EventResponse eventResponse() {
		return new EventResponse(
				1L,
				"Coldplay - Brasilia",
				"Music of the Spheres Tour",
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"),
				EventStatus.AVAILABLE,
				Instant.parse("2026-10-09T18:00:00Z"));
	}
}
