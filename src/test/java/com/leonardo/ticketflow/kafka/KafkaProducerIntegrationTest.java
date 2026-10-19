package com.leonardo.ticketflow.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.leonardo.ticketflow.kafka.config.KafkaTopicConfig;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.producer.PaymentEventProducer;

@Testcontainers
@SpringBootTest(
		classes = {
				KafkaAutoConfiguration.class,
				KafkaTopicConfig.class,
				PaymentEventProducer.class
		},
		properties = {
				"ticketflow.kafka.topic-partitions=1",
				"ticketflow.kafka.replication-factor=1",
				"spring.kafka.admin.fail-fast=true"
		})
class KafkaProducerIntegrationTest {

	@Container
	static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.1");

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	private final PaymentEventProducer paymentEventProducer;

	@Autowired
	KafkaProducerIntegrationTest(PaymentEventProducer paymentEventProducer) {
		this.paymentEventProducer = paymentEventProducer;
	}

	@Test
	void shouldPublishPaymentApprovedEventToKafka() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-19T12:00:00Z"));

		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
			consumer.subscribe(List.of(KafkaTopics.PAYMENT_APPROVED));
			awaitAssignment(consumer);

			paymentEventProducer.publish(event);

			ConsumerRecord<String, String> record = awaitRecord(consumer);
			assertThat(record.key()).isEqualTo("50");
			assertThat(record.value())
					.contains("\"paymentId\":80")
					.contains("\"reservationId\":50")
					.contains("\"userId\":1")
					.contains("\"eventId\":10")
					.contains("\"amount\":250.00")
					.contains("\"occurredAt\":\"2026-10-19T12:00:00Z\"");
		}
	}

	private Map<String, Object> consumerProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-approved-test-" + UUID.randomUUID());
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return properties;
	}

	private void awaitAssignment(KafkaConsumer<String, String> consumer) {
		Instant deadline = Instant.now().plusSeconds(10);
		while (consumer.assignment().isEmpty() && Instant.now().isBefore(deadline)) {
			consumer.poll(Duration.ofMillis(250));
		}
		assertThat(consumer.assignment()).isNotEmpty();
	}

	private ConsumerRecord<String, String> awaitRecord(KafkaConsumer<String, String> consumer) {
		Instant deadline = Instant.now().plusSeconds(10);
		while (Instant.now().isBefore(deadline)) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
			for (ConsumerRecord<String, String> record : records) {
				if (record.topic().equals(KafkaTopics.PAYMENT_APPROVED)) {
					return record;
				}
			}
		}
		throw new AssertionError("Payment approved event was not received from Kafka");
	}
}
