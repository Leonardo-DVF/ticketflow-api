package com.leonardo.ticketflow.kafka.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import com.leonardo.ticketflow.kafka.config.KafkaTopics;

class KafkaEventSerializationTest {

	private final JacksonJsonSerializer<Object> serializer = new JacksonJsonSerializer<>();

	@Test
	void shouldSerializeReservationCreatedWithJavaTimeFields() {
		ReservationCreatedEvent event = new ReservationCreatedEvent(
				50L,
				1L,
				10L,
				List.of(100L, 101L),
				new BigDecimal("500.00"),
				Instant.parse("2026-10-16T15:10:00Z"),
				Instant.parse("2026-10-16T15:00:00Z"));

		String json = serialize(KafkaTopics.RESERVATION_CREATED, event);

		assertThat(json)
				.contains("\"reservationId\":50")
				.contains("\"expiresAt\"")
				.contains("\"occurredAt\"");
	}

	@Test
	void shouldSerializeReservationExpiredWithJavaTimeField() {
		ReservationExpiredEvent event = new ReservationExpiredEvent(
				50L,
				1L,
				10L,
				List.of(100L),
				Instant.parse("2026-10-16T15:10:00Z"));

		String json = serialize(KafkaTopics.RESERVATION_EXPIRED, event);

		assertThat(json)
				.contains("\"reservationId\":50")
				.contains("\"expiredAt\"");
	}

	private String serialize(String topic, Object event) {
		return new String(serializer.serialize(topic, event), StandardCharsets.UTF_8);
	}
}
