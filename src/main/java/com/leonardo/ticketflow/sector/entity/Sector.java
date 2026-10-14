package com.leonardo.ticketflow.sector.entity;

import java.math.BigDecimal;

import com.leonardo.ticketflow.event.entity.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sectors")
public class Sector {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	protected Sector() {
	}

	public Sector(Event event, String name, BigDecimal price) {
		this.event = event;
		this.name = name;
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public Event getEvent() {
		return event;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getPrice() {
		return price;
	}
}
