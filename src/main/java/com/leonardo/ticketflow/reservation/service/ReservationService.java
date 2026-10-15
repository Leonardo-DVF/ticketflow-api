package com.leonardo.ticketflow.reservation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
	private final Clock clock;

	public ReservationService(
			UserRepository userRepository,
			EventRepository eventRepository,
			SeatRepository seatRepository,
			ReservationRepository reservationRepository,
			ReservationItemRepository reservationItemRepository,
			Clock clock) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.seatRepository = seatRepository;
		this.reservationRepository = reservationRepository;
		this.reservationItemRepository = reservationItemRepository;
		this.clock = clock;
	}

	@Transactional
	public ReservationResponse create(String userEmail, CreateReservationRequest request) {
		List<Long> seatIds = validateAndSortSeatIds(request.seatIds());
		User user = userRepository.findByEmailIgnoreCase(userEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
		Event event = eventRepository.findByIdAndStatus(request.eventId(), EventStatus.AVAILABLE)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventId()));

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
