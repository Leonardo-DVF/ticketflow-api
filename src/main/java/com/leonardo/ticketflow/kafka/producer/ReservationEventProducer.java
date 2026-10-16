package com.leonardo.ticketflow.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.ReservationCreatedEvent;
import com.leonardo.ticketflow.kafka.event.ReservationExpiredEvent;

@Component
public class ReservationEventProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReservationEventProducer.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public ReservationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(ReservationCreatedEvent event) {
		publish(KafkaTopics.RESERVATION_CREATED, event.reservationId(), event);
	}

	public void publish(ReservationExpiredEvent event) {
		publish(KafkaTopics.RESERVATION_EXPIRED, event.reservationId(), event);
	}

	private void publish(String topic, Long reservationId, Object event) {
		String key = reservationId.toString();
		try {
			kafkaTemplate.send(topic, key, event)
					.whenComplete((result, exception) -> {
						if (exception != null) {
							LOGGER.error(
									"Failed to publish reservation event to topic {} with key {}",
									topic,
									key,
									exception);
						}
					});
		} catch (RuntimeException exception) {
			LOGGER.error(
					"Failed to publish reservation event to topic {} with key {}",
					topic,
					key,
					exception);
		}
	}
}
