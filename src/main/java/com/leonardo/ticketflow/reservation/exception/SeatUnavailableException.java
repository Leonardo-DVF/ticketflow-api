package com.leonardo.ticketflow.reservation.exception;

public class SeatUnavailableException extends RuntimeException {

	public SeatUnavailableException(String seatCode) {
		super("Seat is not available: " + seatCode);
	}
}
