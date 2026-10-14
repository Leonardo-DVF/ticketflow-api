package com.leonardo.ticketflow.seat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSeatRequest(
		@NotBlank(message = "Code is required")
		@Size(max = 50, message = "Code must not exceed 50 characters")
		String code) {
}
