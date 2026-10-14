package com.leonardo.ticketflow.sector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private SectorRepository sectorRepository;

	private SectorService sectorService;

	@BeforeEach
	void setUp() {
		sectorService = new SectorService(eventRepository, sectorRepository);
	}

	@Test
	void shouldCreateSectorForDraftEvent() {
		Event event = draftEvent();
		CreateSectorRequest request = new CreateSectorRequest("  VIP  ", new BigDecimal("250.00"));
		when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
		when(sectorRepository.existsByEventIdAndNameIgnoreCase(1L, "VIP")).thenReturn(false);
		when(sectorRepository.saveAndFlush(any(Sector.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SectorResponse response = sectorService.create(1L, request);

		assertThat(response.name()).isEqualTo("VIP");
		assertThat(response.price()).isEqualByComparingTo("250.00");
	}

	@Test
	void shouldRejectSectorForPublishedEvent() {
		Event event = draftEvent();
		event.changeStatus(EventStatus.AVAILABLE);
		when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

		assertThatThrownBy(() -> sectorService.create(1L, request("VIP")))
				.isInstanceOf(InvalidSectorException.class)
				.hasMessage("Sectors can only be added to draft events");
	}

	@Test
	void shouldRejectDuplicatedSectorNameBeforeSaving() {
		when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent()));
		when(sectorRepository.existsByEventIdAndNameIgnoreCase(1L, "VIP")).thenReturn(true);

		assertThatThrownBy(() -> sectorService.create(1L, request("VIP")))
				.isInstanceOf(SectorAlreadyExistsException.class)
				.hasMessage("Sector already exists in this event: VIP");
	}

	@Test
	void shouldTranslateDatabaseRaceToDuplicateSectorError() {
		when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent()));
		when(sectorRepository.existsByEventIdAndNameIgnoreCase(1L, "VIP")).thenReturn(false);
		when(sectorRepository.saveAndFlush(any(Sector.class)))
				.thenThrow(new DataIntegrityViolationException(
						"duplicate key violates uk_sectors_event_name"));

		assertThatThrownBy(() -> sectorService.create(1L, request("VIP")))
				.isInstanceOf(SectorAlreadyExistsException.class)
				.hasMessage("Sector already exists in this event: VIP");
	}

	@Test
	void shouldListSectorsForExistingEvent() {
		Event event = org.mockito.Mockito.mock(Event.class);
		when(event.getId()).thenReturn(1L);
		Sector sector = org.mockito.Mockito.mock(Sector.class);
		when(sector.getId()).thenReturn(10L);
		when(sector.getEvent()).thenReturn(event);
		when(sector.getName()).thenReturn("VIP");
		when(sector.getPrice()).thenReturn(new BigDecimal("250.00"));
		when(eventRepository.existsById(1L)).thenReturn(true);
		when(sectorRepository.findAllByEventIdOrderByNameAsc(1L)).thenReturn(List.of(sector));

		List<SectorResponse> response = sectorService.findByEvent(1L);

		assertThat(response).singleElement().satisfies(item -> {
			assertThat(item.id()).isEqualTo(10L);
			assertThat(item.eventId()).isEqualTo(1L);
			assertThat(item.name()).isEqualTo("VIP");
		});
	}

	@Test
	void shouldRejectSectorListingForUnknownEvent() {
		when(eventRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> sectorService.findByEvent(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Event not found: 99");
	}

	private Event draftEvent() {
		return new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
	}

	private CreateSectorRequest request(String name) {
		return new CreateSectorRequest(name, new BigDecimal("250.00"));
	}
}
