package com.leonardo.ticketflow.seat.exception;

public class SeatAlreadyExistsException extends RuntimeException {

	public SeatAlreadyExistsException(String code) {
		super("Seat already exists in this sector: " + code);
	}
}
