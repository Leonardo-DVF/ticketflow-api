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

import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class PaymentApprovedNotificationConsumerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private PaymentApprovedNotificationConsumer consumer;

	@Test
	void shouldDelegateApprovedPaymentForNotification() {
		PaymentApprovedEvent event = event();

		consumer.consume(event);

		verify(notificationService).sendPaymentApproved(event);
	}

	@Test
	void shouldUseIndependentNotificationConsumerGroup() throws Exception {
		Method consume = PaymentApprovedNotificationConsumer.class.getMethod(
				"consume",
				PaymentApprovedEvent.class);
		KafkaListener listener = consume.getAnnotation(KafkaListener.class);

		assertThat(listener.topics()).containsExactly(KafkaTopics.PAYMENT_APPROVED);
		assertThat(listener.groupId()).isEqualTo(KafkaConsumerGroups.NOTIFICATION);
		assertThat(listener.groupId()).isNotEqualTo(KafkaConsumerGroups.TICKET);
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
