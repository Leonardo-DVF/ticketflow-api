package com.leonardo.ticketflow.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findAllBySectorIdOrderByCodeAsc(Long sectorId);

	List<Seat> findAllBySectorEventIdOrderBySectorNameAscCodeAsc(Long eventId);

	List<Seat> findAllBySectorEventIdAndStatusOrderBySectorNameAscCodeAsc(
			Long eventId,
			SeatStatus status);

	boolean existsBySectorIdAndCodeIgnoreCase(Long sectorId, String code);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT seat
			FROM Seat seat
			JOIN FETCH seat.sector sector
			JOIN FETCH sector.event event
			WHERE seat.id IN :seatIds
			ORDER BY seat.id
			""")
	List<Seat> findAllByIdWithLock(@Param("seatIds") List<Long> seatIds);
}
