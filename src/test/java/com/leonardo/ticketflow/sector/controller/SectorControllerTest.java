package com.leonardo.ticketflow.sector.controller;

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
import com.leonardo.ticketflow.sector.dto.CreateSectorRequest;
import com.leonardo.ticketflow.sector.dto.SectorResponse;
import com.leonardo.ticketflow.sector.exception.SectorAlreadyExistsException;
import com.leonardo.ticketflow.sector.service.SectorService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SectorController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class SectorControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SectorService sectorService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldAllowAdminToCreateSector() throws Exception {
		CreateSectorRequest request = validRequest();
		when(sectorService.create(eq(1L), any(CreateSectorRequest.class)))
				.thenReturn(response());

		mockMvc.perform(post("/events/1/sectors")
				.with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.eventId").value(1))
				.andExpect(jsonPath("$.name").value("VIP"))
				.andExpect(jsonPath("$.price").value(250.00));
	}

	@Test
	void shouldForbidRegularUserFromCreatingSector() throws Exception {
		mockMvc.perform(post("/events/1/sectors")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Access denied"));

		verifyNoInteractions(sectorService);
	}

	@Test
	void shouldAllowAdminToListSectors() throws Exception {
		when(sectorService.findByEvent(1L)).thenReturn(List.of(response()));

		mockMvc.perform(get("/events/1/sectors").with(admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(10))
				.andExpect(jsonPath("$[0].name").value("VIP"));
	}

	@Test
	void shouldForbidRegularUserFromListingSectors() throws Exception {
		mockMvc.perform(get("/events/1/sectors")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Access denied"));

		verifyNoInteractions(sectorService);
	}

	@Test
	void shouldRejectInvalidSectorData() throws Exception {
		CreateSectorRequest request = new CreateSectorRequest("", new BigDecimal("0.00"));

		mockMvc.perform(post("/events/1/sectors")
				.with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/events/1/sectors"));

		verifyNoInteractions(sectorService);
	}

	@Test
	void shouldReturnConflictForDuplicatedSector() throws Exception {
		when(sectorService.create(eq(1L), any(CreateSectorRequest.class)))
				.thenThrow(new SectorAlreadyExistsException("VIP"));

		mockMvc.perform(post("/events/1/sectors")
				.with(admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message")
						.value("Sector already exists in this event: VIP"));
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	private CreateSectorRequest validRequest() {
		return new CreateSectorRequest("VIP", new BigDecimal("250.00"));
	}

	private SectorResponse response() {
		return new SectorResponse(10L, 1L, "VIP", new BigDecimal("250.00"));
	}
}
