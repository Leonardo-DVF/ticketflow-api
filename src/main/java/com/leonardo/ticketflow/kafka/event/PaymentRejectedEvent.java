package com.leonardo.ticketflow.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRejectedEvent(
		Long paymentId,
		Long reservationId,
		Long userId,
		Long eventId,
		BigDecimal amount,
		Instant occurredAt) {
}
