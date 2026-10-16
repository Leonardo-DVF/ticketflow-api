package com.leonardo.ticketflow.kafka.event;

import java.time.Instant;
import java.util.List;

public record ReservationExpiredEvent(
		Long reservationId,
		Long userId,
		Long eventId,
		List<Long> seatIds,
		Instant expiredAt) {

	public ReservationExpiredEvent {
		seatIds = List.copyOf(seatIds);
	}
}
