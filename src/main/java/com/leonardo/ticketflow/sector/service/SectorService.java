package com.leonardo.ticketflow.sector.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;
import com.leonardo.ticketflow.sector.dto.CreateSectorRequest;
import com.leonardo.ticketflow.sector.dto.SectorResponse;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.exception.InvalidSectorException;
import com.leonardo.ticketflow.sector.exception.SectorAlreadyExistsException;
import com.leonardo.ticketflow.sector.repository.SectorRepository;

@Service
public class SectorService {

	private final EventRepository eventRepository;
	private final SectorRepository sectorRepository;

	public SectorService(EventRepository eventRepository, SectorRepository sectorRepository) {
		this.eventRepository = eventRepository;
		this.sectorRepository = sectorRepository;
	}

	@Transactional
	public SectorResponse create(Long eventId, CreateSectorRequest request) {
		Event event = findEvent(eventId);
		if (event.getStatus() != EventStatus.DRAFT) {
			throw new InvalidSectorException("Sectors can only be added to draft events");
		}

		String normalizedName = request.name().trim();
		if (sectorRepository.existsByEventIdAndNameIgnoreCase(eventId, normalizedName)) {
			throw new SectorAlreadyExistsException(normalizedName);
		}

		try {
			Sector sector = sectorRepository.saveAndFlush(
					new Sector(event, normalizedName, request.price()));
			return toResponse(sector);
		} catch (DataIntegrityViolationException exception) {
			String databaseMessage = exception.getMostSpecificCause().getMessage();
			if (databaseMessage != null && databaseMessage.contains("uk_sectors_event_name")) {
				throw new SectorAlreadyExistsException(normalizedName);
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<SectorResponse> findByEvent(Long eventId) {
		if (!eventRepository.existsById(eventId)) {
			throw new ResourceNotFoundException("Event not found: " + eventId);
		}

		return sectorRepository.findAllByEventIdOrderByNameAsc(eventId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private Event findEvent(Long eventId) {
		return eventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
	}

	private SectorResponse toResponse(Sector sector) {
		return new SectorResponse(
				sector.getId(),
				sector.getEvent().getId(),
				sector.getName(),
				sector.getPrice());
	}
}
