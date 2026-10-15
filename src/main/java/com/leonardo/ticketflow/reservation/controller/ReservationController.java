package com.leonardo.ticketflow.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.service.ReservationService;

import jakarta.validation.Valid;
import java.util.List;

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

	@GetMapping("/{id}")
	public ReservationResponse findById(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id) {
		return reservationService.findById(jwt.getSubject(), id);
	}

	@GetMapping("/my")
	public List<ReservationResponse> findMine(@AuthenticationPrincipal Jwt jwt) {
		return reservationService.findMine(jwt.getSubject());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id) {
		reservationService.cancel(jwt.getSubject(), id);
	}
}
