package com.leonardo.ticketflow.kafka.config;

public final class KafkaConsumerGroups {

	public static final String TICKET = "ticket-service";
	public static final String NOTIFICATION = "notification-service";
	public static final String AUDIT = "audit-service";

	private KafkaConsumerGroups() {
	}
}
