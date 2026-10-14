package com.leonardo.ticketflow.event.dto;

import java.time.OffsetDateTime;

import com.leonardo.ticketflow.event.entity.EventStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 160, message = "Name must not exceed 160 characters")
		String name,

		@Size(max = 5000, message = "Description must not exceed 5000 characters")
		String description,

		@NotBlank(message = "Location is required")
		@Size(max = 255, message = "Location must not exceed 255 characters")
		String location,

		@NotNull(message = "Event date is required")
		@Future(message = "Event date must be in the future")
		OffsetDateTime eventDate,

		@NotNull(message = "Status is required")
		EventStatus status) {
}
