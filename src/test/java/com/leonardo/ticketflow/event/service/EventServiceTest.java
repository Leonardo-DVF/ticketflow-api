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
}
