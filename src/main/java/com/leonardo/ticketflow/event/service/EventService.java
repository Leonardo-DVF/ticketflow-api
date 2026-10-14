package com.leonardo.ticketflow.event.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.event.dto.CreateEventRequest;
import com.leonardo.ticketflow.event.dto.EventResponse;
import com.leonardo.ticketflow.event.dto.UpdateEventRequest;
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
		validateFutureDate(request.eventDate());

		Event event = new Event(
				request.name().trim(),
				normalizeDescription(request.description()),
				request.location().trim(),
				request.eventDate());
		Event savedEvent = eventRepository.save(event);

		return toResponse(savedEvent);
	}

	@Transactional
	public EventResponse update(Long id, UpdateEventRequest request) {
		Event event = findById(id);
		validateEditable(event);
		validateStatusTransition(event.getStatus(), request.status());
		validateFutureDate(request.eventDate());

		event.update(
				request.name().trim(),
				normalizeDescription(request.description()),
				request.location().trim(),
				request.eventDate());
		event.changeStatus(request.status());

		return toResponse(event);
	}

	@Transactional
	public void cancel(Long id) {
		Event event = findById(id);
		if (event.getStatus() == EventStatus.FINISHED) {
			throw new InvalidEventException("Finished event cannot be cancelled");
		}

		event.changeStatus(EventStatus.CANCELLED);
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

	private Event findById(Long id) {
		return eventRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
	}

	private void validateFutureDate(OffsetDateTime eventDate) {
		if (!eventDate.toInstant().isAfter(clock.instant())) {
			throw new InvalidEventException("Event date must be in the future");
		}
	}

	private void validateEditable(Event event) {
		if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
			throw new InvalidEventException(
					"Event cannot be updated when status is " + event.getStatus());
		}
	}

	private void validateStatusTransition(EventStatus currentStatus, EventStatus requestedStatus) {
		if (requestedStatus == EventStatus.CANCELLED) {
			throw new InvalidEventException("Use the cancellation endpoint to cancel an event");
		}
		if (requestedStatus == EventStatus.FINISHED) {
			throw new InvalidEventException("Event cannot be manually marked as finished");
		}
		if (currentStatus == EventStatus.AVAILABLE && requestedStatus == EventStatus.DRAFT) {
			throw new InvalidEventException("Published event cannot return to draft");
		}
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
