package com.leonardo.ticketflow.kafka.producer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	private PaymentEventProducer producer;

	@BeforeEach
	void setUp() {
		producer = new PaymentEventProducer(kafkaTemplate);
	}

	@Test
	void shouldPublishApprovedPaymentUsingReservationIdAsKey() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));
		when(kafkaTemplate.send(KafkaTopics.PAYMENT_APPROVED, "50", event))
				.thenReturn(completedSend());

		producer.publish(event);

		verify(kafkaTemplate).send(KafkaTopics.PAYMENT_APPROVED, "50", event);
	}

	@Test
	void shouldPublishRejectedPaymentUsingReservationIdAsKey() {
		PaymentRejectedEvent event = new PaymentRejectedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));
		when(kafkaTemplate.send(KafkaTopics.PAYMENT_REJECTED, "50", event))
				.thenReturn(completedSend());

		producer.publish(event);

		verify(kafkaTemplate).send(KafkaTopics.PAYMENT_REJECTED, "50", event);
	}

	private CompletableFuture<SendResult<String, Object>> completedSend() {
		return CompletableFuture.completedFuture(null);
	}
}
