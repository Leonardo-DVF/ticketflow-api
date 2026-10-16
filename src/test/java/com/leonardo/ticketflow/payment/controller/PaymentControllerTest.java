package com.leonardo.ticketflow.payment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

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
import com.leonardo.ticketflow.payment.dto.PaymentResponse;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;
import com.leonardo.ticketflow.payment.exception.InvalidPaymentException;
import com.leonardo.ticketflow.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentService paymentService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldApprovePaymentForAuthenticatedUser() throws Exception {
		when(paymentService.approve("leonardo@example.com", 50L))
				.thenReturn(response(PaymentStatus.APPROVED));

		mockMvc.perform(post("/payments/50/approve")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(80))
				.andExpect(jsonPath("$.reservationId").value(50))
				.andExpect(jsonPath("$.amount").value(250.00))
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void shouldRejectPaymentForAuthenticatedUser() throws Exception {
		when(paymentService.reject("leonardo@example.com", 50L))
				.thenReturn(response(PaymentStatus.REJECTED));

		mockMvc.perform(post("/payments/50/reject")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		verify(paymentService).reject("leonardo@example.com", 50L);
	}

	@Test
	void shouldRequireAuthentication() throws Exception {
		mockMvc.perform(post("/payments/50/approve"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(paymentService);
	}

	@Test
	void shouldReturnConflictForInvalidPayment() throws Exception {
		when(paymentService.approve("leonardo@example.com", 50L))
				.thenThrow(new InvalidPaymentException(
						"Reservation already has an approved payment"));

		mockMvc.perform(post("/payments/50/approve")
				.with(jwt().jwt(jwt -> jwt.subject("leonardo@example.com"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Reservation already has an approved payment"));
	}

	private PaymentResponse response(PaymentStatus status) {
		return new PaymentResponse(
				80L,
				50L,
				new BigDecimal("250.00"),
				status,
				Instant.parse("2026-10-16T15:00:00Z"));
	}
}
