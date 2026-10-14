package com.leonardo.ticketflow.seat.service;

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
import com.leonardo.ticketflow.seat.dto.CreateSeatRequest;
import com.leonardo.ticketflow.seat.dto.SeatResponse;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.exception.InvalidSeatException;
import com.leonardo.ticketflow.seat.exception.SeatAlreadyExistsException;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.repository.SectorRepository;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private SectorRepository sectorRepository;

	@Mock
	private SeatRepository seatRepository;

	private SeatService seatService;

	@BeforeEach
	void setUp() {
		seatService = new SeatService(eventRepository, sectorRepository, seatRepository);
	}

	@Test
	void shouldCreateAvailableSeatForDraftEvent() {
		Sector sector = sectorWithEvent(EventStatus.DRAFT);
		when(sectorRepository.findById(10L)).thenReturn(Optional.of(sector));
		when(seatRepository.existsBySectorIdAndCodeIgnoreCase(10L, "A1")).thenReturn(false);
		when(seatRepository.saveAndFlush(any(Seat.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SeatResponse response = seatService.create(10L, new CreateSeatRequest("  A1  "));

		assertThat(response.code()).isEqualTo("A1");
		assertThat(response.status()).isEqualTo(SeatStatus.AVAILABLE);
		assertThat(response.sectorName()).isEqualTo("VIP");
		assertThat(response.price()).isEqualByComparingTo("250.00");
	}

	@Test
	void shouldRejectSeatForPublishedEvent() {
		when(sectorRepository.findById(10L))
				.thenReturn(Optional.of(sectorWithEvent(EventStatus.AVAILABLE)));

		assertThatThrownBy(() -> seatService.create(10L, request()))
				.isInstanceOf(InvalidSeatException.class)
				.hasMessage("Seats can only be added to draft events");
	}

	@Test
	void shouldRejectUnknownSector() {
		when(sectorRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> seatService.create(99L, request()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Sector not found: 99");
	}

	@Test
	void shouldRejectDuplicatedSeatCodeBeforeSaving() {
		when(sectorRepository.findById(10L))
				.thenReturn(Optional.of(sectorWithEvent(EventStatus.DRAFT)));
		when(seatRepository.existsBySectorIdAndCodeIgnoreCase(10L, "A1")).thenReturn(true);

		assertThatThrownBy(() -> seatService.create(10L, request()))
				.isInstanceOf(SeatAlreadyExistsException.class)
				.hasMessage("Seat already exists in this sector: A1");
	}

	@Test
	void shouldTranslateDatabaseRaceToDuplicateSeatError() {
		when(sectorRepository.findById(10L))
				.thenReturn(Optional.of(sectorWithEvent(EventStatus.DRAFT)));
		when(seatRepository.existsBySectorIdAndCodeIgnoreCase(10L, "A1")).thenReturn(false);
		when(seatRepository.saveAndFlush(any(Seat.class)))
				.thenThrow(new DataIntegrityViolationException(
						"duplicate key violates uk_seats_sector_code"));

		assertThatThrownBy(() -> seatService.create(10L, request()))
				.isInstanceOf(SeatAlreadyExistsException.class)
				.hasMessage("Seat already exists in this sector: A1");
	}

	@Test
	void shouldListAllSeatsForPublishedEvent() {
		Event event = publishedEvent();
		Seat seat = seat(SeatStatus.AVAILABLE);
		when(eventRepository.findByIdAndStatus(1L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(event));
		when(seatRepository.findAllBySectorEventIdOrderBySectorNameAscCodeAsc(1L))
				.thenReturn(List.of(seat));

		List<SeatResponse> response = seatService.findByEvent(1L);

		assertThat(response).singleElement().satisfies(item -> {
			assertThat(item.code()).isEqualTo("A1");
			assertThat(item.status()).isEqualTo(SeatStatus.AVAILABLE);
		});
	}

	@Test
	void shouldListOnlyAvailableSeatsForPublishedEvent() {
		Event event = publishedEvent();
		Seat seat = seat(SeatStatus.AVAILABLE);
		when(eventRepository.findByIdAndStatus(1L, EventStatus.AVAILABLE))
				.thenReturn(Optional.of(event));
		when(seatRepository.findAllBySectorEventIdAndStatusOrderBySectorNameAscCodeAsc(
				1L,
				SeatStatus.AVAILABLE))
				.thenReturn(List.of(seat));

		List<SeatResponse> response = seatService.findAvailableByEvent(1L);

		assertThat(response).extracting(SeatResponse::status)
				.containsOnly(SeatStatus.AVAILABLE);
	}

	@Test
	void shouldHideSeatsFromUnpublishedEvent() {
		when(eventRepository.findByIdAndStatus(1L, EventStatus.AVAILABLE))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> seatService.findByEvent(1L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Event not found: 1");
	}

	private CreateSeatRequest request() {
		return new CreateSeatRequest("A1");
	}

	private Event publishedEvent() {
		Event event = event(EventStatus.AVAILABLE);
		return event;
	}

	private Sector sectorWithEvent(EventStatus status) {
		return new Sector(event(status), "VIP", new BigDecimal("250.00"));
	}

	private Event event(EventStatus status) {
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		event.changeStatus(status);
		return event;
	}

	private Seat seat(SeatStatus status) {
		Sector sector = org.mockito.Mockito.mock(Sector.class);
		Seat seat = org.mockito.Mockito.mock(Seat.class);
		when(sector.getId()).thenReturn(10L);
		when(sector.getName()).thenReturn("VIP");
		when(sector.getPrice()).thenReturn(new BigDecimal("250.00"));
		when(seat.getId()).thenReturn(100L);
		when(seat.getSector()).thenReturn(sector);
		when(seat.getCode()).thenReturn("A1");
		when(seat.getStatus()).thenReturn(status);
		return seat;
	}
}
