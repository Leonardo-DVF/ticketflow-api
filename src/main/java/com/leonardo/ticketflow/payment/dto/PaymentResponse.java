package com.leonardo.ticketflow.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.leonardo.ticketflow.payment.entity.PaymentStatus;

public record PaymentResponse(
		Long id,
		Long reservationId,
		BigDecimal amount,
		PaymentStatus status,
		Instant createdAt) {
}
