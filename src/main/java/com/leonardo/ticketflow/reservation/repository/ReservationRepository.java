package com.leonardo.ticketflow.reservation.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Optional<Reservation> findByIdAndUserId(Long id, Long userId);

	List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<Reservation> findByIdAndUserEmailIgnoreCase(Long id, String email);

	List<Reservation> findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT reservation
			FROM Reservation reservation
			WHERE reservation.id = :id
			AND LOWER(reservation.user.email) = LOWER(:email)
			""")
	Optional<Reservation> findOwnedByIdWithLock(
			@Param("id") Long id,
			@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT reservation FROM Reservation reservation WHERE reservation.id = :id")
	Optional<Reservation> findByIdWithLock(@Param("id") Long id);

	List<Reservation> findAllByStatusAndExpiresAtBefore(
			ReservationStatus status,
			Instant expiresAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT reservation
			FROM Reservation reservation
			WHERE reservation.status = :status
			AND reservation.expiresAt <= :now
			AND NOT EXISTS (
				SELECT payment.id
				FROM Payment payment
				WHERE payment.reservation = reservation
				AND payment.status = :approvedPaymentStatus
			)
			ORDER BY reservation.id
			""")
	List<Reservation> findAllDueWithLock(
			@Param("status") ReservationStatus status,
			@Param("now") Instant now,
			@Param("approvedPaymentStatus") PaymentStatus approvedPaymentStatus);
}
