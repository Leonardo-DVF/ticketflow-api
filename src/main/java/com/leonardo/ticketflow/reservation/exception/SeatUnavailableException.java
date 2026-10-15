package com.leonardo.ticketflow.reservation.exception;

public class SeatUnavailableException extends RuntimeException {

	public SeatUnavailableException(String seatCode) {
		super("Seat is not available: " + seatCode);
	}

	public SeatUnavailableException(Long seatId) {
		super("Seat is currently reserved: " + seatId);
	}
}
