package com.leonardo.ticketflow.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.leonardo.ticketflow.event.dto.CreateEventRequest;
import com.leonardo.ticketflow.event.dto.EventResponse;
import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.exception.InvalidEventException;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");

	@Mock
	private EventRepository eventRepository;

	private EventService eventService;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
		eventService = new EventService(eventRepository, fixedClock);
	}

	@Test
	void shouldCreateDraftEventWithNormalizedData() {
		OffsetDateTime eventDate = OffsetDateTime.parse("2026-12-20T20:00:00-03:00");
		CreateEventRequest request = new CreateEventRequest(
				"  Coldplay - Brasilia  ",
				"  Coldplay live in Brasilia  ",
				"  Arena BRB Mane Garrincha  ",
				eventDate);
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventResponse response = eventService.create(request);

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(eventRepository).save(eventCaptor.capture());
		Event savedEvent = eventCaptor.getValue();

		assertThat(savedEvent.getName()).isEqualTo("Coldplay - Brasilia");
		assertThat(savedEvent.getDescription()).isEqualTo("Coldplay live in Brasilia");
		assertThat(savedEvent.getLocation()).isEqualTo("Arena BRB Mane Garrincha");
		assertThat(savedEvent.getEventDate()).isEqualTo(eventDate);
		assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
		assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
	}

	@Test
	void shouldNormalizeBlankDescriptionToNull() {
		CreateEventRequest request = new CreateEventRequest(
				"Coldplay - Brasilia",
				"   ",
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventResponse response = eventService.create(request);

		assertThat(response.description()).isNull();
	}

	@Test
	void shouldRejectEventThatIsNotInTheFuture() {
		CreateEventRequest request = new CreateEventRequest(
				"Past event",
				null,
				"Brasilia",
				OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

		assertThatThrownBy(() -> eventService.create(request))
				.isInstanceOf(InvalidEventException.class)
				.hasMessage("Event date must be in the future");

		verify(eventRepository, never()).save(any(Event.class));
	}

	@Test
	void shouldListOnlyAvailableEventsOrderedByDate() {
		Event firstEvent = availableEvent(
				1L,
				"Coldplay - Brasilia",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		Event secondEvent = availableEvent(
				2L,
				"Coldplay - Sao Paulo",
				OffsetDateTime.parse("2026-12-22T20:00:00-03:00"));
		when(eventRepository.findAllByStatusOrderByEventDateAsc(EventStatus.AVAILABLE))
				.thenReturn(List.of(firstEvent, secondEvent));

		List<EventResponse> response = eventService.findAvailable();

		assertThat(response).extracting(EventResponse::id).containsExactly(1L, 2L);
		assertThat(response).extracting(EventResponse::status)
				.containsOnly(EventStatus.AVAILABLE);
	}

	@Test
	void shouldFindAvailableEventById() {
		Event event = availableEvent(
				1L,
				"Coldplay - Brasilia",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		when(eventRepository.findByIdAndStatus(1L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(event));

		EventResponse response = eventService.findAvailableById(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Coldplay - Brasilia");
		assertThat(response.status()).isEqualTo(EventStatus.AVAILABLE);
	}

	@Test
	void shouldRejectUnavailableOrUnknownEvent() {
		when(eventRepository.findByIdAndStatus(99L, EventStatus.AVAILABLE))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.findAvailableById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Event not found: 99");
	}

	private Event availableEvent(Long id, String name, OffsetDateTime eventDate) {
		Event event = org.mockito.Mockito.mock(Event.class);
		when(event.getId()).thenReturn(id);
		when(event.getName()).thenReturn(name);
		when(event.getDescription()).thenReturn("Live concert");
		when(event.getLocation()).thenReturn("Brasilia");
		when(event.getEventDate()).thenReturn(eventDate);
		when(event.getStatus()).thenReturn(EventStatus.AVAILABLE);
		when(event.getCreatedAt()).thenReturn(NOW);
		return event;
	}
}
