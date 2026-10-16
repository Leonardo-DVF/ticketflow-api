package com.leonardo.ticketflow.reservation.exception;

public class ReservationExpiredException extends RuntimeException {

	public ReservationExpiredException(String message) {
		super(message);
	}
}
