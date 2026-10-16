package com.leonardo.ticketflow.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record TicketIssuedEvent(
		Long ticketId,
		Long reservationId,
		Long userId,
		Long eventId,
		Long seatId,
		UUID code,
		Instant issuedAt) {
}
