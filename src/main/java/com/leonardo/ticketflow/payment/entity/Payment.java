package com.leonardo.ticketflow.payment.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.leonardo.ticketflow.reservation.entity.Reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Payment() {
	}

	public Payment(Reservation reservation, BigDecimal amount) {
		this.reservation = reservation;
		this.amount = amount;
		this.status = PaymentStatus.PENDING;
	}

	public void approve() {
		ensurePending();
		status = PaymentStatus.APPROVED;
	}

	public void reject() {
		ensurePending();
		status = PaymentStatus.REJECTED;
	}

	private void ensurePending() {
		if (status != PaymentStatus.PENDING) {
			throw new IllegalStateException("Only pending payments can be processed");
		}
	}

	@PrePersist
	void assignInitialValues() {
		if (status == null) {
			status = PaymentStatus.PENDING;
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
