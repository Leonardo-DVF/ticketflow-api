package com.leonardo.ticketflow.kafka.config;

public final class KafkaTopics {

	public static final String RESERVATION_CREATED = "reservation-created";
	public static final String RESERVATION_EXPIRED = "reservation-expired";
	public static final String PAYMENT_APPROVED = "payment-approved";
	public static final String PAYMENT_REJECTED = "payment-rejected";
	public static final String TICKET_ISSUED = "ticket-issued";

	private KafkaTopics() {
	}
}
