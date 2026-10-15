package com.leonardo.ticketflow.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateReservationRequest request) {
		return reservationService.create(jwt.getSubject(), request);
	}
}
