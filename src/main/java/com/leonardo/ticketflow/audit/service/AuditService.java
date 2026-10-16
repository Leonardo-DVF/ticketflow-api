package com.leonardo.ticketflow.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.audit.entity.AuditEventType;
import com.leonardo.ticketflow.audit.entity.AuditLog;
import com.leonardo.ticketflow.audit.repository.AuditLogRepository;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;

@Service
public class AuditService {

	private final AuditLogRepository auditLogRepository;

	public AuditService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void recordPaymentApproved(PaymentApprovedEvent event) {
		if (auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_APPROVED,
				event.paymentId())) {
			return;
		}

		auditLogRepository.save(new AuditLog(
				AuditEventType.PAYMENT_APPROVED,
				event.paymentId(),
				event.reservationId(),
				event.userId(),
				event.eventId(),
				event.amount(),
				event.occurredAt()));
	}
}
