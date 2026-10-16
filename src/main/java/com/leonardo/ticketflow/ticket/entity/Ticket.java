package com.leonardo.ticketflow.ticket.entity;

import java.time.Instant;
import java.util.UUID;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@Column(nullable = false, unique = true, updatable = false)
	private UUID code;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Ticket() {
	}

	public Ticket(Reservation reservation, Seat seat) {
		this.reservation = reservation;
		this.user = reservation.getUser();
		this.event = reservation.getEvent();
		this.seat = seat;
		this.code = UUID.randomUUID();
	}

	@PrePersist
	void assignInitialValues() {
		if (code == null) {
			code = UUID.randomUUID();
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

	public User getUser() {
		return user;
	}

	public Event getEvent() {
		return event;
	}

	public Seat getSeat() {
		return seat;
	}

	public UUID getCode() {
		return code;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
