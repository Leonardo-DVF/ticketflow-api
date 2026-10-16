package com.leonardo.ticketflow.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReservationCreatedEvent(
		Long reservationId,
		Long userId,
		Long eventId,
		List<Long> seatIds,
		BigDecimal totalAmount,
		Instant expiresAt,
		Instant occurredAt) {

	public ReservationCreatedEvent {
		seatIds = List.copyOf(seatIds);
	}
}
