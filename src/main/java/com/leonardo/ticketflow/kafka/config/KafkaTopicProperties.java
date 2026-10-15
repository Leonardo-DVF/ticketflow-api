package com.leonardo.ticketflow.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketflow.kafka")
public record KafkaTopicProperties(int topicPartitions, short replicationFactor) {

	public KafkaTopicProperties {
		if (topicPartitions < 1) {
			throw new IllegalArgumentException("Kafka topic partitions must be greater than zero");
		}
		if (replicationFactor < 1) {
			throw new IllegalArgumentException("Kafka replication factor must be greater than zero");
		}
	}
}
