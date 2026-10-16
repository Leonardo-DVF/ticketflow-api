package com.leonardo.ticketflow.payment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.payment.dto.PaymentResponse;
import com.leonardo.ticketflow.payment.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/{reservationId}/approve")
	public PaymentResponse approve(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long reservationId) {
		return paymentService.approve(jwt.getSubject(), reservationId);
	}

	@PostMapping("/{reservationId}/reject")
	public PaymentResponse reject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long reservationId) {
		return paymentService.reject(jwt.getSubject(), reservationId);
	}
}
