package com.leonardo.ticketflow.reservation.entity;

import java.math.BigDecimal;

import com.leonardo.ticketflow.seat.entity.Seat;

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
@Table(name = "reservation_items")
public class ReservationItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	protected ReservationItem() {
	}

	public ReservationItem(Reservation reservation, Seat seat, BigDecimal unitPrice) {
		this.reservation = reservation;
		this.seat = seat;
		this.unitPrice = unitPrice;
	}

	public Long getId() {
		return id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public Seat getSeat() {
		return seat;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
}
