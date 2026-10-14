package com.leonardo.ticketflow.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findAllBySectorIdOrderByCodeAsc(Long sectorId);

	List<Seat> findAllBySectorEventIdAndStatusOrderBySectorNameAscCodeAsc(
			Long eventId,
			SeatStatus status);

	boolean existsBySectorIdAndCodeIgnoreCase(Long sectorId, String code);
}
