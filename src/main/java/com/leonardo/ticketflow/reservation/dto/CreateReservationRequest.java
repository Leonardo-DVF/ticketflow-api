package com.leonardo.ticketflow.reservation.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
		@NotNull(message = "Event id is required")
		@Positive(message = "Event id must be positive")
		Long eventId,

		@NotEmpty(message = "At least one seat is required")
		@Size(max = 10, message = "A reservation can contain at most 10 seats")
		List<@NotNull(message = "Seat id is required") @Positive(message = "Seat id must be positive") Long> seatIds) {
}
