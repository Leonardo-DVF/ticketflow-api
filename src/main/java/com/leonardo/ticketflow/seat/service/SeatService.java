package com.leonardo.ticketflow.seat.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.seat.dto.CreateSeatRequest;
import com.leonardo.ticketflow.seat.dto.SeatResponse;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.exception.InvalidSeatException;
import com.leonardo.ticketflow.seat.exception.SeatAlreadyExistsException;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.repository.SectorRepository;

@Service
public class SeatService {

	private final EventRepository eventRepository;
	private final SectorRepository sectorRepository;
	private final SeatRepository seatRepository;

	public SeatService(
			EventRepository eventRepository,
			SectorRepository sectorRepository,
			SeatRepository seatRepository) {
		this.eventRepository = eventRepository;
		this.sectorRepository = sectorRepository;
		this.seatRepository = seatRepository;
	}

	@Transactional
	public SeatResponse create(Long sectorId, CreateSeatRequest request) {
		Sector sector = sectorRepository.findById(sectorId)
				.orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + sectorId));
		if (sector.getEvent().getStatus() != EventStatus.DRAFT) {
			throw new InvalidSeatException("Seats can only be added to draft events");
		}

		String normalizedCode = request.code().trim();
		if (seatRepository.existsBySectorIdAndCodeIgnoreCase(sectorId, normalizedCode)) {
			throw new SeatAlreadyExistsException(normalizedCode);
		}

		try {
			Seat seat = seatRepository.saveAndFlush(new Seat(sector, normalizedCode));
			return toResponse(seat);
		} catch (DataIntegrityViolationException exception) {
			String databaseMessage = exception.getMostSpecificCause().getMessage();
			if (databaseMessage != null && databaseMessage.contains("uk_seats_sector_code")) {
				throw new SeatAlreadyExistsException(normalizedCode);
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<SeatResponse> findByEvent(Long eventId) {
		validatePublishedEvent(eventId);
		return seatRepository.findAllBySectorEventIdOrderBySectorNameAscCodeAsc(eventId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SeatResponse> findAvailableByEvent(Long eventId) {
		validatePublishedEvent(eventId);
		return seatRepository.findAllBySectorEventIdAndStatusOrderBySectorNameAscCodeAsc(
				eventId,
				SeatStatus.AVAILABLE)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private void validatePublishedEvent(Long eventId) {
		if (eventRepository.findByIdAndStatus(eventId, EventStatus.AVAILABLE).isEmpty()) {
			throw new ResourceNotFoundException("Event not found: " + eventId);
		}
	}

	private SeatResponse toResponse(Seat seat) {
		Sector sector = seat.getSector();
		return new SeatResponse(
				seat.getId(),
				seat.getCode(),
				seat.getStatus(),
				sector.getId(),
				sector.getName(),
				sector.getPrice());
	}
}
