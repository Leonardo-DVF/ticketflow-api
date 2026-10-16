package com.leonardo.ticketflow.ticket.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(
		Long id,
		UUID code,
		Long reservationId,
		Long eventId,
		String eventName,
		String eventLocation,
		OffsetDateTime eventDate,
		Long seatId,
		String seatCode,
		String sectorName,
		Instant createdAt) {
}
