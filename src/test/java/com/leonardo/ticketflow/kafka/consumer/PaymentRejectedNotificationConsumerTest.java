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
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;
import com.leonardo.ticketflow.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class PaymentRejectedNotificationConsumerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private PaymentRejectedNotificationConsumer consumer;

	@Test
	void shouldDelegateRejectedPaymentForNotification() {
		PaymentRejectedEvent event = event();

		consumer.consume(event);

		verify(notificationService).sendPaymentRejected(event);
	}

	@Test
	void shouldListenToRejectedPaymentsInNotificationGroup() throws Exception {
		Method consume = PaymentRejectedNotificationConsumer.class.getMethod(
				"consume",
				PaymentRejectedEvent.class);
		KafkaListener listener = consume.getAnnotation(KafkaListener.class);

		assertThat(listener.topics()).containsExactly(KafkaTopics.PAYMENT_REJECTED);
		assertThat(listener.groupId()).isEqualTo(KafkaConsumerGroups.NOTIFICATION);
	}

	private PaymentRejectedEvent event() {
		return new PaymentRejectedEvent(
				81L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));
	}
}
