package com.leonardo.ticketflow.reservation.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.repository.SeatRepository;

@Service
public class ReservationExpirationService {

	private final ReservationRepository reservationRepository;
	private final ReservationItemRepository reservationItemRepository;
	private final SeatRepository seatRepository;
	private final RedisSeatLockService seatLockService;
	private final Clock clock;

	public ReservationExpirationService(
			ReservationRepository reservationRepository,
			ReservationItemRepository reservationItemRepository,
			SeatRepository seatRepository,
			RedisSeatLockService seatLockService,
			Clock clock) {
		this.reservationRepository = reservationRepository;
		this.reservationItemRepository = reservationItemRepository;
		this.seatRepository = seatRepository;
		this.seatLockService = seatLockService;
		this.clock = clock;
	}

	@Transactional
	public int expireDueReservations() {
		List<Reservation> reservations = reservationRepository.findAllDueWithLock(
				ReservationStatus.PENDING,
				clock.instant());

		for (Reservation reservation : reservations) {
			List<Long> seatIds = reservationItemRepository
					.findAllByReservationIdOrderById(reservation.getId())
					.stream()
					.map(ReservationItem::getSeat)
					.map(Seat::getId)
					.sorted()
					.toList();
			seatRepository.findAllByIdWithLock(seatIds).forEach(Seat::release);
			reservation.expire();
			Long eventId = reservation.getEvent().getId();
			String lockOwner = "reservation:" + reservation.getId();
			TransactionActions.afterCommit(() -> seatIds.forEach(
					seatId -> releaseLockQuietly(eventId, seatId, lockOwner)));
		}

		return reservations.size();
	}

	private void releaseLockQuietly(Long eventId, Long seatId, String owner) {
		try {
			seatLockService.release(eventId, seatId, owner);
		} catch (RuntimeException ignored) {
			// The TTL is the final safeguard when Redis is unavailable during cleanup.
		}
	}
}
