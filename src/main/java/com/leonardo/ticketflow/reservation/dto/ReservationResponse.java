package com.leonardo.ticketflow.reservation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.leonardo.ticketflow.reservation.entity.ReservationStatus;

public record ReservationResponse(
		Long id,
		Long eventId,
		ReservationStatus status,
		Instant expiresAt,
		BigDecimal totalAmount,
		Instant createdAt,
		List<ReservationItemResponse> items) {
}
