package com.leonardo.ticketflow.sector.dto;

import java.math.BigDecimal;

public record SectorResponse(
		Long id,
		Long eventId,
		String name,
		BigDecimal price) {
}
