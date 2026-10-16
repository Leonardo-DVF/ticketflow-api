package com.leonardo.ticketflow.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.audit.service.AuditService;
import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;

@Component
public class PaymentRejectedAuditConsumer {

	private final AuditService auditService;

	public PaymentRejectedAuditConsumer(AuditService auditService) {
		this.auditService = auditService;
	}

	@KafkaListener(
			topics = KafkaTopics.PAYMENT_REJECTED,
			groupId = KafkaConsumerGroups.AUDIT)
	public void consume(PaymentRejectedEvent event) {
		auditService.recordPaymentRejected(event);
	}
}
