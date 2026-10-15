package com.leonardo.ticketflow.reservation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationItemResponse;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.entity.Reservation;
import com.leonardo.ticketflow.reservation.entity.ReservationItem;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.InvalidReservationException;
import com.leonardo.ticketflow.reservation.exception.ReservationNotCancellableException;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Service
public class ReservationService {

	private static final Duration RESERVATION_DURATION = Duration.ofMinutes(10);

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationItemRepository reservationItemRepository;
	private final RedisSeatLockService seatLockService;
	private final Clock clock;

	public ReservationService(
			UserRepository userRepository,
			EventRepository eventRepository,
			SeatRepository seatRepository,
			ReservationRepository reservationRepository,
			ReservationItemRepository reservationItemRepository,
			RedisSeatLockService seatLockService,
			Clock clock) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.seatRepository = seatRepository;
		this.reservationRepository = reservationRepository;
		this.reservationItemRepository = reservationItemRepository;
		this.seatLockService = seatLockService;
		this.clock = clock;
	}

	@Transactional
	public ReservationResponse create(String userEmail, CreateReservationRequest request) {
		List<Long> seatIds = validateAndSortSeatIds(request.seatIds());
		Map<Long, String> lockOwners = acquireLocks(request.eventId(), seatIds);
		TransactionActions.afterRollback(
				() -> releaseLocks(request.eventId(), lockOwners));
		try {
			return createReservation(userEmail, request.eventId(), seatIds, lockOwners);
		} catch (RuntimeException exception) {
			releaseLocks(request.eventId(), lockOwners);
			throw exception;
		}
	}

	private ReservationResponse createReservation(
			String userEmail,
			Long eventId,
			List<Long> seatIds,
			Map<Long, String> lockOwners) {
		User user = userRepository.findByEmailIgnoreCase(userEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
		Event event = eventRepository.findByIdAndStatus(eventId, EventStatus.AVAILABLE)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

		List<Seat> seats = seatRepository.findAllByIdWithLock(seatIds);
		if (seats.size() != seatIds.size()) {
			throw new InvalidReservationException("One or more seats were not found");
		}

		validateSeats(event, seats);
		BigDecimal totalAmount = seats.stream()
				.map(seat -> seat.getSector().getPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Instant expiresAt = clock.instant().plus(RESERVATION_DURATION);
		Reservation reservation = reservationRepository.save(
				new Reservation(user, event, expiresAt, totalAmount));

		List<ReservationItem> items = new ArrayList<>();
		for (Seat seat : seats) {
			seat.reserve();
			items.add(new ReservationItem(reservation, seat, seat.getSector().getPrice()));
		}
		reservationItemRepository.saveAll(items);
		transferLocks(eventId, reservation.getId(), lockOwners);

		return toResponse(reservation, items);
	}

	@Transactional(readOnly = true)
	public ReservationResponse findById(String userEmail, Long reservationId) {
		Reservation reservation = reservationRepository
				.findByIdAndUserEmailIgnoreCase(reservationId, userEmail)
				.orElseThrow(() -> reservationNotFound(reservationId));
		List<ReservationItem> items = reservationItemRepository
				.findAllByReservationIdOrderById(reservationId);
		return toResponse(reservation, items);
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> findMine(String userEmail) {
		return reservationRepository
				.findAllByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail)
				.stream()
				.map(reservation -> toResponse(
						reservation,
						reservationItemRepository.findAllByReservationIdOrderById(
								reservation.getId())))
				.toList();
	}

	@Transactional
	public void cancel(String userEmail, Long reservationId) {
		Reservation reservation = reservationRepository
				.findOwnedByIdWithLock(reservationId, userEmail)
				.orElseThrow(() -> reservationNotFound(reservationId));
		if (reservation.getStatus() != ReservationStatus.PENDING) {
			throw new ReservationNotCancellableException(
					"Only pending reservations can be cancelled");
		}

		List<ReservationItem> items = reservationItemRepository
				.findAllByReservationIdOrderById(reservationId);
		List<Long> seatIds = items.stream()
				.map(item -> item.getSeat().getId())
				.sorted()
				.toList();
		List<Seat> seats = seatRepository.findAllByIdWithLock(seatIds);
		seats.forEach(Seat::release);
		reservation.cancel();
		Long eventId = reservation.getEvent().getId();
		String lockOwner = reservationOwner(reservationId);
		TransactionActions.afterCommit(() -> seatIds.forEach(
				seatId -> releaseLockQuietly(eventId, seatId, lockOwner)));
	}

	private Map<Long, String> acquireLocks(Long eventId, List<Long> seatIds) {
		String temporaryOwner = "pending:" + UUID.randomUUID();
		Map<Long, String> lockOwners = new LinkedHashMap<>();
		try {
			for (Long seatId : seatIds) {
				if (!seatLockService.acquire(eventId, seatId, temporaryOwner)) {
					throw new SeatUnavailableException(seatId);
				}
				lockOwners.put(seatId, temporaryOwner);
			}
			return lockOwners;
		} catch (RuntimeException exception) {
			releaseLocks(eventId, lockOwners);
			throw exception;
		}
	}

	private void transferLocks(
			Long eventId,
			Long reservationId,
			Map<Long, String> lockOwners) {
		String reservationOwner = reservationOwner(reservationId);
		for (Map.Entry<Long, String> lock : lockOwners.entrySet()) {
			if (!seatLockService.transfer(
					eventId,
					lock.getKey(),
					lock.getValue(),
					reservationOwner)) {
				throw new IllegalStateException(
						"Could not associate seat lock with reservation");
			}
			lock.setValue(reservationOwner);
		}
	}

	private void releaseLocks(Long eventId, Map<Long, String> lockOwners) {
		lockOwners.forEach((seatId, owner) -> releaseLockQuietly(eventId, seatId, owner));
	}

	private void releaseLockQuietly(Long eventId, Long seatId, String owner) {
		try {
			seatLockService.release(eventId, seatId, owner);
		} catch (RuntimeException ignored) {
			// The TTL is the final safeguard when Redis is unavailable during cleanup.
		}
	}

	private String reservationOwner(Long reservationId) {
		return "reservation:" + reservationId;
	}

	private ResourceNotFoundException reservationNotFound(Long reservationId) {
		return new ResourceNotFoundException("Reservation not found: " + reservationId);
	}

	private List<Long> validateAndSortSeatIds(List<Long> seatIds) {
		if (new HashSet<>(seatIds).size() != seatIds.size()) {
			throw new InvalidReservationException("Seat ids must not be duplicated");
		}
		return seatIds.stream().sorted().toList();
	}

	private void validateSeats(Event event, List<Seat> seats) {
		for (Seat seat : seats) {
			if (!seat.getSector().getEvent().getId().equals(event.getId())) {
				throw new InvalidReservationException("All seats must belong to the selected event");
			}
			if (seat.getStatus() != SeatStatus.AVAILABLE) {
				throw new SeatUnavailableException(seat.getCode());
			}
		}
	}

	private ReservationResponse toResponse(
			Reservation reservation,
			List<ReservationItem> items) {
		List<ReservationItemResponse> itemResponses = items.stream()
				.map(item -> new ReservationItemResponse(
						item.getSeat().getId(),
						item.getSeat().getCode(),
						item.getSeat().getSector().getName(),
						item.getUnitPrice()))
				.toList();

		return new ReservationResponse(
				reservation.getId(),
				reservation.getEvent().getId(),
				reservation.getStatus(),
				reservation.getExpiresAt(),
				reservation.getTotalAmount(),
				reservation.getCreatedAt(),
				itemResponses);
	}
}
