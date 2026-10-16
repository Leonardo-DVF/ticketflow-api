package com.leonardo.ticketflow.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.notification.service.NotificationService;

@Component
public class PaymentApprovedNotificationConsumer {

	private final NotificationService notificationService;

	public PaymentApprovedNotificationConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@KafkaListener(
			topics = KafkaTopics.PAYMENT_APPROVED,
			groupId = KafkaConsumerGroups.NOTIFICATION)
	public void consume(PaymentApprovedEvent event) {
		notificationService.sendPaymentApproved(event);
	}
}
