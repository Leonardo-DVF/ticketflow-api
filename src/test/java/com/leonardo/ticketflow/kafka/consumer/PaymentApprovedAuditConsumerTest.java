package com.leonardo.ticketflow.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import com.leonardo.ticketflow.audit.service.AuditService;
import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentApprovedAuditConsumerTest {

	@Mock
	private AuditService auditService;

	@InjectMocks
	private PaymentApprovedAuditConsumer consumer;

	@Test
	void shouldDelegateApprovedPaymentForAudit() {
		PaymentApprovedEvent event = event();

		consumer.consume(event);

		verify(auditService).recordPaymentApproved(event);
	}

	@Test
	void shouldUseIndependentAuditConsumerGroup() throws Exception {
		Method consume = PaymentApprovedAuditConsumer.class.getMethod(
				"consume",
				PaymentApprovedEvent.class);
		KafkaListener listener = consume.getAnnotation(KafkaListener.class);

		assertThat(listener.topics()).containsExactly(KafkaTopics.PAYMENT_APPROVED);
		assertThat(listener.groupId()).isEqualTo(KafkaConsumerGroups.AUDIT);
		assertThat(listener.groupId()).isNotIn(
				KafkaConsumerGroups.TICKET,
				KafkaConsumerGroups.NOTIFICATION);
	}

	private PaymentApprovedEvent event() {
		return new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));
	}
}
