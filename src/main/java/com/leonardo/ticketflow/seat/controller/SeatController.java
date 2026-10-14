package com.leonardo.ticketflow.seat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.seat.dto.CreateSeatRequest;
import com.leonardo.ticketflow.seat.dto.SeatResponse;
import com.leonardo.ticketflow.seat.service.SeatService;

import jakarta.validation.Valid;

@RestController
public class SeatController {

	private final SeatService seatService;

	public SeatController(SeatService seatService) {
		this.seatService = seatService;
	}

	@PostMapping("/sectors/{sectorId}/seats")
	@ResponseStatus(HttpStatus.CREATED)
	public SeatResponse create(
			@PathVariable Long sectorId,
			@Valid @RequestBody CreateSeatRequest request) {
		return seatService.create(sectorId, request);
	}

	@GetMapping("/events/{eventId}/seats")
	public List<SeatResponse> findByEvent(@PathVariable Long eventId) {
		return seatService.findByEvent(eventId);
	}

	@GetMapping("/events/{eventId}/seats/available")
	public List<SeatResponse> findAvailableByEvent(@PathVariable Long eventId) {
		return seatService.findAvailableByEvent(eventId);
	}
}
