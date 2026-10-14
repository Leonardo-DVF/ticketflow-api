package com.leonardo.ticketflow.sector.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSectorRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must not exceed 100 characters")
		String name,

		@NotNull(message = "Price is required")
		@Positive(message = "Price must be greater than zero")
		@Digits(integer = 10, fraction = 2, message = "Price must have up to 10 integer and 2 decimal digits")
		BigDecimal price) {
}
