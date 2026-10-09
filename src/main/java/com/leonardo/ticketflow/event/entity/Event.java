package com.leonardo.ticketflow.event.entity;

import java.time.Instant;
import java.time.OffsetDateTime;

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
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 255)
	private String location;

	@Column(name = "event_date", nullable = false)
	private OffsetDateTime eventDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EventStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Event() {
	}

	public Event(String name, String description, String location, OffsetDateTime eventDate) {
		this.name = name;
		this.description = description;
		this.location = location;
		this.eventDate = eventDate;
		this.status = EventStatus.DRAFT;
	}

	@PrePersist
	void assignCreationTime() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}

		if (status == null) {
			status = EventStatus.DRAFT;
		}
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public OffsetDateTime getEventDate() {
		return eventDate;
	}

	public EventStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
