package com.leonardo.ticketflow.kafka.producer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.ReservationCreatedEvent;
import com.leonardo.ticketflow.kafka.event.ReservationExpiredEvent;

@ExtendWith(MockitoExtension.class)
class ReservationEventProducerTest {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ReservationEventProducer producer;

	@BeforeEach
	void setUp() {
		producer = new ReservationEventProducer(kafkaTemplate);
	}

	@Test
	void shouldPublishReservationCreatedUsingReservationIdAsKey() {
		ReservationCreatedEvent event = new ReservationCreatedEvent(
				50L,
				1L,
				10L,
				List.of(100L),
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:10:00Z"),
				Instant.parse("2026-10-16T15:00:00Z"));
		when(kafkaTemplate.send(KafkaTopics.RESERVATION_CREATED, "50", event))
				.thenReturn(completedSend());

		producer.publish(event);

		verify(kafkaTemplate).send(KafkaTopics.RESERVATION_CREATED, "50", event);
	}

	@Test
	void shouldPublishReservationExpiredUsingReservationIdAsKey() {
		ReservationExpiredEvent event = new ReservationExpiredEvent(
				50L,
				1L,
				10L,
				List.of(100L),
				Instant.parse("2026-10-16T15:10:00Z"));
		when(kafkaTemplate.send(KafkaTopics.RESERVATION_EXPIRED, "50", event))
				.thenReturn(completedSend());

		producer.publish(event);

		verify(kafkaTemplate).send(KafkaTopics.RESERVATION_EXPIRED, "50", event);
	}

	private CompletableFuture<SendResult<String, Object>> completedSend() {
		return CompletableFuture.completedFuture(null);
	}
}
