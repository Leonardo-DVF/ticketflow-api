package com.leonardo.ticketflow.event.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.event.dto.CreateEventRequest;
import com.leonardo.ticketflow.event.dto.EventResponse;
import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.exception.InvalidEventException;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;

@Service
public class EventService {

	private final EventRepository eventRepository;
	private final Clock clock;

	public EventService(EventRepository eventRepository, Clock clock) {
		this.eventRepository = eventRepository;
		this.clock = clock;
	}

	@Transactional
	public EventResponse create(CreateEventRequest request) {
		if (!request.eventDate().toInstant().isAfter(clock.instant())) {
			throw new InvalidEventException("Event date must be in the future");
		}

		Event event = new Event(
				request.name().trim(),
				normalizeDescription(request.description()),
				request.location().trim(),
				request.eventDate());
		Event savedEvent = eventRepository.save(event);

		return toResponse(savedEvent);
	}

	@Transactional(readOnly = true)
	public List<EventResponse> findAvailable() {
		return eventRepository.findAllByStatusOrderByEventDateAsc(EventStatus.AVAILABLE)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public EventResponse findAvailableById(Long id) {
		Event event = eventRepository.findByIdAndStatus(id, EventStatus.AVAILABLE)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
		return toResponse(event);
	}

	private String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}

	private EventResponse toResponse(Event event) {
		return new EventResponse(
				event.getId(),
				event.getName(),
				event.getDescription(),
				event.getLocation(),
				event.getEventDate(),
				event.getStatus(),
				event.getCreatedAt());
	}
}
