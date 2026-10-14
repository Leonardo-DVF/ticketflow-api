package com.leonardo.ticketflow.sector.exception;

public class SectorAlreadyExistsException extends RuntimeException {

	public SectorAlreadyExistsException(String name) {
		super("Sector already exists in this event: " + name);
	}
}
