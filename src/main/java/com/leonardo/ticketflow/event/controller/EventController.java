package com.leonardo.ticketflow.event.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.event.dto.CreateEventRequest;
import com.leonardo.ticketflow.event.dto.EventResponse;
import com.leonardo.ticketflow.event.dto.UpdateEventRequest;
import com.leonardo.ticketflow.event.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
		return eventService.create(request);
	}

	@GetMapping
	public List<EventResponse> findAvailable() {
		return eventService.findAvailable();
	}

	@GetMapping("/{id}")
	public EventResponse findAvailableById(@PathVariable Long id) {
		return eventService.findAvailableById(id);
	}

	@PutMapping("/{id}")
	public EventResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateEventRequest request) {
		return eventService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable Long id) {
		eventService.cancel(id);
	}
}
