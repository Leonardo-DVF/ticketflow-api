package com.leonardo.ticketflow.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException(String email) {
		super("Email is already registered: " + email);
	}

	public EmailAlreadyRegisteredException(String email, Throwable cause) {
		super("Email is already registered: " + email, cause);
	}
}
