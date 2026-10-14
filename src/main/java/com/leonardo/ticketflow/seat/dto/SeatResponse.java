package com.leonardo.ticketflow.seat.dto;

import java.math.BigDecimal;

import com.leonardo.ticketflow.seat.entity.SeatStatus;

public record SeatResponse(
		Long id,
		String code,
		SeatStatus status,
		Long sectorId,
		String sectorName,
		BigDecimal price) {
}
