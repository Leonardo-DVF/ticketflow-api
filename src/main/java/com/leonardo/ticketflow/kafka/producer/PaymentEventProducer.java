package com.leonardo.ticketflow.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;

@Component
public class PaymentEventProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventProducer.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(PaymentApprovedEvent event) {
		publish(KafkaTopics.PAYMENT_APPROVED, event.reservationId(), event);
	}

	public void publish(PaymentRejectedEvent event) {
		publish(KafkaTopics.PAYMENT_REJECTED, event.reservationId(), event);
	}

	private void publish(String topic, Long reservationId, Object event) {
		String key = reservationId.toString();
		try {
			kafkaTemplate.send(topic, key, event)
					.whenComplete((result, exception) -> {
						if (exception != null) {
							LOGGER.error(
									"Failed to publish payment event to topic {} with key {}",
									topic,
									key,
									exception);
						}
					});
		} catch (RuntimeException exception) {
			LOGGER.error(
					"Failed to publish payment event to topic {} with key {}",
					topic,
					key,
					exception);
		}
	}
}
