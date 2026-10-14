package com.leonardo.ticketflow.seat.entity;

import com.leonardo.ticketflow.sector.entity.Sector;

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
@Table(name = "seats")
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sector_id", nullable = false)
	private Sector sector;

	@Column(nullable = false, length = 50)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SeatStatus status;

	protected Seat() {
	}

	public Seat(Sector sector, String code) {
		this.sector = sector;
		this.code = code;
		this.status = SeatStatus.AVAILABLE;
	}

	@PrePersist
	void assignInitialStatus() {
		if (status == null) {
			status = SeatStatus.AVAILABLE;
		}
	}

	public Long getId() {
		return id;
	}

	public Sector getSector() {
		return sector;
	}

	public String getCode() {
		return code;
	}

	public SeatStatus getStatus() {
		return status;
	}
}
