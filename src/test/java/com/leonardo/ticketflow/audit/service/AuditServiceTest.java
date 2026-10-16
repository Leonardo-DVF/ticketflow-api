package com.leonardo.ticketflow.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.leonardo.ticketflow.audit.entity.AuditEventType;
import com.leonardo.ticketflow.audit.entity.AuditLog;
import com.leonardo.ticketflow.audit.repository.AuditLogRepository;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.kafka.event.PaymentRejectedEvent;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	private AuditService auditService;

	@BeforeEach
	void setUp() {
		auditService = new AuditService(auditLogRepository);
	}

	@Test
	void shouldRecordApprovedPayment() {
		PaymentApprovedEvent event = event();
		when(auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_APPROVED,
				80L))
				.thenReturn(false);

		auditService.recordPaymentApproved(event);

		ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(logCaptor.capture());
		AuditLog log = logCaptor.getValue();
		assertThat(log.getEventType()).isEqualTo(AuditEventType.PAYMENT_APPROVED);
		assertThat(log.getAggregateId()).isEqualTo(80L);
		assertThat(log.getReservationId()).isEqualTo(50L);
		assertThat(log.getUserId()).isEqualTo(1L);
		assertThat(log.getEventId()).isEqualTo(10L);
		assertThat(log.getAmount()).isEqualByComparingTo("250.00");
		assertThat(log.getOccurredAt()).isEqualTo(event.occurredAt());
	}

	@Test
	void shouldIgnorePreviouslyRecordedEvent() {
		when(auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_APPROVED,
				80L))
				.thenReturn(true);

		auditService.recordPaymentApproved(event());

		verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void shouldRecordRejectedPayment() {
		PaymentRejectedEvent event = rejectedEvent();
		when(auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_REJECTED,
				81L))
				.thenReturn(false);

		auditService.recordPaymentRejected(event);

		ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(logCaptor.capture());
		AuditLog log = logCaptor.getValue();
		assertThat(log.getEventType()).isEqualTo(AuditEventType.PAYMENT_REJECTED);
		assertThat(log.getAggregateId()).isEqualTo(81L);
		assertThat(log.getReservationId()).isEqualTo(50L);
		assertThat(log.getUserId()).isEqualTo(1L);
		assertThat(log.getEventId()).isEqualTo(10L);
		assertThat(log.getAmount()).isEqualByComparingTo("250.00");
		assertThat(log.getOccurredAt()).isEqualTo(event.occurredAt());
	}

	@Test
	void shouldIgnorePreviouslyRecordedRejectedPayment() {
		when(auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_REJECTED,
				81L))
				.thenReturn(true);

		auditService.recordPaymentRejected(rejectedEvent());

		verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

	private PaymentRejectedEvent rejectedEvent() {
		return new PaymentRejectedEvent(
				81L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:05:00Z"));
	}
}
