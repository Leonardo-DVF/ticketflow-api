package com.leonardo.ticketflow.event.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.leonardo.ticketflow.event.entity.EventStatus;

public record EventResponse(
		Long id,
		String name,
		String description,
		String location,
		OffsetDateTime eventDate,
		EventStatus status,
		Instant createdAt) {
}
