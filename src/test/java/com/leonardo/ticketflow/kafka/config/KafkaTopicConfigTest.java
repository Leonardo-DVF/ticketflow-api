package com.leonardo.ticketflow.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaTopicConfigTest {

	private final KafkaTopicConfig config = new KafkaTopicConfig();
	private final KafkaTopicProperties properties = new KafkaTopicProperties(3, (short) 1);

	@Test
	void shouldDeclareAllApplicationTopics() {
		List<NewTopic> topics = List.of(
				config.reservationCreatedTopic(properties),
				config.reservationExpiredTopic(properties),
				config.paymentApprovedTopic(properties),
				config.paymentRejectedTopic(properties),
				config.ticketIssuedTopic(properties));

		assertThat(topics)
				.extracting(NewTopic::name)
				.containsExactly(
						KafkaTopics.RESERVATION_CREATED,
						KafkaTopics.RESERVATION_EXPIRED,
						KafkaTopics.PAYMENT_APPROVED,
						KafkaTopics.PAYMENT_REJECTED,
						KafkaTopics.TICKET_ISSUED);
		assertThat(topics).allSatisfy(topic -> {
			assertThat(topic.numPartitions()).isEqualTo(3);
			assertThat(topic.replicationFactor()).isEqualTo((short) 1);
		});
	}
}
