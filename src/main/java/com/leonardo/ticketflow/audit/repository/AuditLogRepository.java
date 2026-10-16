package com.leonardo.ticketflow.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.audit.entity.AuditEventType;
import com.leonardo.ticketflow.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	boolean existsByEventTypeAndAggregateId(
			AuditEventType eventType,
			Long aggregateId);
}
