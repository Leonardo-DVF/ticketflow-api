package com.leonardo.ticketflow.reservation.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Optional<Reservation> findByIdAndUserId(Long id, Long userId);

	List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	List<Reservation> findAllByStatusAndExpiresAtBefore(
			ReservationStatus status,
			Instant expiresAt);
}
