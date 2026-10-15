package com.leonardo.ticketflow.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

	@Bean
	NewTopic reservationCreatedTopic(KafkaTopicProperties properties) {
		return createTopic(KafkaTopics.RESERVATION_CREATED, properties);
	}

	@Bean
	NewTopic reservationExpiredTopic(KafkaTopicProperties properties) {
		return createTopic(KafkaTopics.RESERVATION_EXPIRED, properties);
	}

	@Bean
	NewTopic paymentApprovedTopic(KafkaTopicProperties properties) {
		return createTopic(KafkaTopics.PAYMENT_APPROVED, properties);
	}

	@Bean
	NewTopic paymentRejectedTopic(KafkaTopicProperties properties) {
		return createTopic(KafkaTopics.PAYMENT_REJECTED, properties);
	}

	@Bean
	NewTopic ticketIssuedTopic(KafkaTopicProperties properties) {
		return createTopic(KafkaTopics.TICKET_ISSUED, properties);
	}

	private NewTopic createTopic(String name, KafkaTopicProperties properties) {
		return TopicBuilder.name(name)
				.partitions(properties.topicPartitions())
				.replicas(properties.replicationFactor())
				.build();
	}
}
