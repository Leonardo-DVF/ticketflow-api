package com.leonardo.ticketflow.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.TicketIssuedEvent;

@Component
public class TicketEventProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(TicketEventProducer.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public TicketEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(TicketIssuedEvent event) {
		String key = event.reservationId().toString();
		try {
			kafkaTemplate.send(KafkaTopics.TICKET_ISSUED, key, event)
					.whenComplete((result, exception) -> {
						if (exception != null) {
							LOGGER.error(
									"Failed to publish ticket event with key {}",
									key,
									exception);
						}
					});
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to publish ticket event with key {}", key, exception);
		}
	}
}
