package com.leonardo.ticketflow.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.leonardo.ticketflow.auth.dto.AccessTokenResponse;
import com.leonardo.ticketflow.auth.dto.LoginRequest;
import com.leonardo.ticketflow.auth.dto.UserRegistrationRequest;
import com.leonardo.ticketflow.auth.dto.UserResponse;
import com.leonardo.ticketflow.auth.service.AuthenticationService;
import com.leonardo.ticketflow.auth.service.UserRegistrationService;
import com.leonardo.ticketflow.config.SecurityConfig;
import com.leonardo.ticketflow.exception.GlobalExceptionHandler;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.exception.EmailAlreadyRegisteredException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserRegistrationService registrationService;

	@MockitoBean
	private AuthenticationService authenticationService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldRegisterUser() throws Exception {
		UserRegistrationRequest request = new UserRegistrationRequest(
				"Leonardo",
				"leonardo@example.com",
				"plain-password");
		UserResponse response = new UserResponse(
				1L,
				"Leonardo",
				"leonardo@example.com",
				Role.USER,
				Instant.parse("2026-10-08T12:00:00Z"));
		when(registrationService.register(any(UserRegistrationRequest.class))).thenReturn(response);

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Leonardo"))
				.andExpect(jsonPath("$.email").value("leonardo@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-08T12:00:00Z"));
	}

	@Test
	void shouldReturnBadRequestForInvalidData() throws Exception {
		UserRegistrationRequest request = new UserRegistrationRequest("", "invalid-email", "short");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message", containsString("name:")))
				.andExpect(jsonPath("$.message", containsString("email:")))
				.andExpect(jsonPath("$.message", containsString("password:")))
				.andExpect(jsonPath("$.path").value("/auth/register"))
				.andExpect(jsonPath("$.timestamp").exists());

		verifyNoInteractions(registrationService);
	}

	@Test
	void shouldReturnConflictForRegisteredEmail() throws Exception {
		UserRegistrationRequest request = new UserRegistrationRequest(
				"Leonardo",
				"leonardo@example.com",
				"plain-password");
		when(registrationService.register(any(UserRegistrationRequest.class)))
				.thenThrow(new EmailAlreadyRegisteredException("leonardo@example.com"));

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message")
						.value("Email is already registered: leonardo@example.com"))
				.andExpect(jsonPath("$.path").value("/auth/register"));
	}

	@Test
	void shouldRequireAuthenticationForOtherRoutes() throws Exception {
		mockMvc.perform(get("/protected-resource"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication is required"))
				.andExpect(jsonPath("$.path").value("/protected-resource"));
	}

	@Test
	void shouldAuthenticateValidCredentials() throws Exception {
		LoginRequest request = new LoginRequest("leonardo@example.com", "plain-password");
		when(authenticationService.authenticate(any(LoginRequest.class)))
				.thenReturn(new AccessTokenResponse("signed.jwt.token", "Bearer", 3600));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("signed.jwt.token"))
				.andExpect(jsonPath("$.type").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600));
	}

	@Test
	void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
		LoginRequest request = new LoginRequest("leonardo@example.com", "wrong-password");
		org.mockito.Mockito.doThrow(new BadCredentialsException("Bad credentials"))
				.when(authenticationService)
				.authenticate(any(LoginRequest.class));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"))
				.andExpect(jsonPath("$.path").value("/auth/login"));
	}
}
