package com.leonardo.ticketflow.kafka.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

	@Test
	void shouldSerializePaymentApprovedEvent() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));

		String json = serialize(KafkaTopics.PAYMENT_APPROVED, event);

		assertThat(json)
				.contains("\"paymentId\":80")
				.contains("\"reservationId\":50")
				.contains("\"amount\":250.00")
				.contains("\"occurredAt\"");
	}

	@Test
	void shouldSerializePaymentRejectedEvent() {
		PaymentRejectedEvent event = new PaymentRejectedEvent(
				81L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));

		String json = serialize(KafkaTopics.PAYMENT_REJECTED, event);

		assertThat(json)
				.contains("\"paymentId\":81")
				.contains("\"reservationId\":50")
				.contains("\"amount\":250.00")
				.contains("\"occurredAt\"");
	}

	@Test
	void shouldSerializeTicketIssuedEvent() {
		TicketIssuedEvent event = new TicketIssuedEvent(
				90L,
				50L,
				1L,
				10L,
				100L,
				UUID.fromString("d4203978-e77a-4f9b-b99c-3c42f865f54c"),
				Instant.parse("2026-10-16T15:00:00Z"));

		String json = serialize(KafkaTopics.TICKET_ISSUED, event);

		assertThat(json)
				.contains("\"ticketId\":90")
				.contains("\"seatId\":100")
				.contains("\"code\":\"d4203978-e77a-4f9b-b99c-3c42f865f54c\"")
				.contains("\"issuedAt\"");
	}

	private String serialize(String topic, Object event) {
		return new String(serializer.serialize(topic, event), StandardCharsets.UTF_8);
	}
}
