package com.leonardo.ticketflow.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.audit.service.AuditService;
import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;

@Component
public class PaymentApprovedAuditConsumer {

	private final AuditService auditService;

	public PaymentApprovedAuditConsumer(AuditService auditService) {
		this.auditService = auditService;
	}

	@KafkaListener(
			topics = KafkaTopics.PAYMENT_APPROVED,
			groupId = KafkaConsumerGroups.AUDIT)
	public void consume(PaymentApprovedEvent event) {
		auditService.recordPaymentApproved(event);
	}
}
