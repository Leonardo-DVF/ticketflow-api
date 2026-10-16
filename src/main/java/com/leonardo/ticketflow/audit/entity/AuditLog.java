package com.leonardo.ticketflow.audit.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 50)
	private AuditEventType eventType;

	@Column(name = "aggregate_id", nullable = false)
	private Long aggregateId;

	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AuditLog() {
	}

	public AuditLog(
			AuditEventType eventType,
			Long aggregateId,
			Long reservationId,
			Long userId,
			Long eventId,
			BigDecimal amount,
			Instant occurredAt) {
		this.eventType = eventType;
		this.aggregateId = aggregateId;
		this.reservationId = reservationId;
		this.userId = userId;
		this.eventId = eventId;
		this.amount = amount;
		this.occurredAt = occurredAt;
	}

	@PrePersist
	void assignCreationTime() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public AuditEventType getEventType() {
		return eventType;
	}

	public Long getAggregateId() {
		return aggregateId;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getEventId() {
		return eventId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
