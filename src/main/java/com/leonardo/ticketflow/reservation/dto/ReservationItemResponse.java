package com.leonardo.ticketflow.reservation.dto;

import java.math.BigDecimal;

public record ReservationItemResponse(
		Long seatId,
		String seatCode,
		String sectorName,
		BigDecimal unitPrice) {
}
