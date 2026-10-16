package com.leonardo.ticketflow.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;
import com.leonardo.ticketflow.notification.service.NotificationService;

@Component
public class PaymentRejectedNotificationConsumer {

	private final NotificationService notificationService;

	public PaymentRejectedNotificationConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@KafkaListener(
			topics = KafkaTopics.PAYMENT_REJECTED,
			groupId = KafkaConsumerGroups.NOTIFICATION)
	public void consume(PaymentRejectedEvent event) {
		notificationService.sendPaymentRejected(event);
	}
}
