package com.leonardo.ticketflow.kafka.producer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.TicketIssuedEvent;

@ExtendWith(MockitoExtension.class)
class TicketEventProducerTest {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	private TicketEventProducer producer;

	@BeforeEach
	void setUp() {
		producer = new TicketEventProducer(kafkaTemplate);
	}

	@Test
	void shouldPublishTicketIssuedUsingReservationIdAsKey() {
		TicketIssuedEvent event = new TicketIssuedEvent(
				90L,
				50L,
				1L,
				10L,
				100L,
				UUID.fromString("d4203978-e77a-4f9b-b99c-3c42f865f54c"),
				Instant.parse("2026-10-16T15:00:00Z"));
		when(kafkaTemplate.send(KafkaTopics.TICKET_ISSUED, "50", event))
				.thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null));

		producer.publish(event);

		verify(kafkaTemplate).send(KafkaTopics.TICKET_ISSUED, "50", event);
	}
}
