package com.leonardo.ticketflow.reservation.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.user.entity.User;

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
@Table(name = "reservations")
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Reservation() {
	}

	public Reservation(User user, Event event, Instant expiresAt, BigDecimal totalAmount) {
		this.user = user;
		this.event = event;
		this.expiresAt = expiresAt;
		this.totalAmount = totalAmount;
		this.status = ReservationStatus.PENDING;
	}

	public void cancel() {
		if (status != ReservationStatus.PENDING) {
			throw new IllegalStateException("Only pending reservations can be cancelled");
		}
		status = ReservationStatus.CANCELLED;
	}

	public void expire() {
		if (status != ReservationStatus.PENDING) {
			throw new IllegalStateException("Only pending reservations can expire");
		}
		status = ReservationStatus.EXPIRED;
	}

	public void confirm() {
		if (status != ReservationStatus.PENDING) {
			throw new IllegalStateException("Only pending reservations can be confirmed");
		}
		status = ReservationStatus.CONFIRMED;
	}

	@PrePersist
	void assignInitialValues() {
		if (status == null) {
			status = ReservationStatus.PENDING;
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Event getEvent() {
		return event;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
