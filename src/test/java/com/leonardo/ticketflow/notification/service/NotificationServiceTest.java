package com.leonardo.ticketflow.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;

@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceTest {

	private final NotificationService notificationService = new NotificationService();

	@Test
	void shouldLogSimulatedPaymentApprovalNotification(CapturedOutput output) {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));

		notificationService.sendPaymentApproved(event);

		assertThat(output.getOut())
				.contains("Notification sent to user 1")
				.contains("approved payment 80")
				.contains("reservation 50");
	}

	@Test
	void shouldLogSimulatedPaymentRejectionNotification(CapturedOutput output) {
		PaymentRejectedEvent event = new PaymentRejectedEvent(
				81L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));

		notificationService.sendPaymentRejected(event);

		assertThat(output.getOut())
				.contains("Notification sent to user 1")
				.contains("rejected payment 81")
				.contains("reservation 50");
	}
}
