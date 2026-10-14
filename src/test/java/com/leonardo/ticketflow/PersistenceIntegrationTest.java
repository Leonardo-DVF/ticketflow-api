package com.leonardo.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.repository.SectorRepository;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Testcontainers
@SpringBootTest(properties =
		"security.jwt.secret=ticketflow-integration-test-secret-key-32-bytes")
@Transactional
class PersistenceIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SectorRepository sectorRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldApplyUserTableMigration() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'users'
				""")
				.query(Integer.class)
				.single();

		assertThat(tableCount).isEqualTo(1);
	}

	@Test
	void shouldPersistAndFindUserByEmailIgnoringCase() {
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"encoded-password",
				Role.USER);

		User savedUser = userRepository.saveAndFlush(user);

		assertThat(savedUser.getId()).isNotNull();
		assertThat(savedUser.getCreatedAt()).isNotNull();
		assertThat(userRepository.findByEmailIgnoreCase("LEONARDO@EXAMPLE.COM"))
				.isPresent()
				.get()
				.extracting(User::getRole)
				.isEqualTo(Role.USER);
	}

	@Test
	void shouldPersistDraftEvent() {
		OffsetDateTime eventDate = OffsetDateTime.parse("2026-12-20T20:00:00-03:00");
		Event event = new Event(
				"Coldplay - Brasilia",
				"Coldplay live in Brasilia",
				"Arena BRB Mane Garrincha",
				eventDate);

		Event savedEvent = eventRepository.saveAndFlush(event);
		Long eventId = savedEvent.getId();
		entityManager.clear();

		Event persistedEvent = eventRepository.findById(eventId).orElseThrow();

		assertThat(persistedEvent.getName()).isEqualTo("Coldplay - Brasilia");
		assertThat(persistedEvent.getDescription()).isEqualTo("Coldplay live in Brasilia");
		assertThat(persistedEvent.getLocation()).isEqualTo("Arena BRB Mane Garrincha");
		assertThat(persistedEvent.getEventDate()).isEqualTo(eventDate);
		assertThat(persistedEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
		assertThat(persistedEvent.getCreatedAt()).isNotNull();
	}

	@Test
	void shouldApplySectorTableMigration() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'sectors'
				""")
				.query(Integer.class)
				.single();

		assertThat(tableCount).isEqualTo(1);
	}

	@Test
	void shouldPersistSectorRelatedToEvent() {
		Event event = eventRepository.saveAndFlush(new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00")));
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "VIP", new BigDecimal("250.00")));
		Long sectorId = sector.getId();
		entityManager.clear();

		Sector persistedSector = sectorRepository.findById(sectorId).orElseThrow();

		assertThat(persistedSector.getName()).isEqualTo("VIP");
		assertThat(persistedSector.getPrice()).isEqualByComparingTo("250.00");
		assertThat(persistedSector.getEvent().getId()).isEqualTo(event.getId());
		assertThat(sectorRepository.findAllByEventIdOrderByNameAsc(event.getId()))
				.extracting(Sector::getName)
				.containsExactly("VIP");
	}

	@Test
	void shouldRejectDuplicateSectorNameIgnoringCaseWithinSameEvent() {
		Event event = eventRepository.saveAndFlush(new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00")));
		sectorRepository.saveAndFlush(new Sector(event, "VIP", new BigDecimal("250.00")));

		assertThatThrownBy(() -> sectorRepository.saveAndFlush(
				new Sector(event, "vip", new BigDecimal("300.00"))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldApplySeatTableMigration() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'seats'
				""")
				.query(Integer.class)
				.single();

		assertThat(tableCount).isEqualTo(1);
	}

	@Test
	void shouldPersistAvailableSeatRelatedToSector() {
		Event event = eventRepository.saveAndFlush(new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00")));
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "VIP", new BigDecimal("250.00")));
		Seat seat = seatRepository.saveAndFlush(new Seat(sector, "A1"));
		Long seatId = seat.getId();
		entityManager.clear();

		Seat persistedSeat = seatRepository.findById(seatId).orElseThrow();

		assertThat(persistedSeat.getCode()).isEqualTo("A1");
		assertThat(persistedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
		assertThat(persistedSeat.getSector().getId()).isEqualTo(sector.getId());
		assertThat(seatRepository.findAllBySectorIdOrderByCodeAsc(sector.getId()))
				.extracting(Seat::getCode)
				.containsExactly("A1");
		assertThat(seatRepository
				.findAllBySectorEventIdAndStatusOrderBySectorNameAscCodeAsc(
						event.getId(),
						SeatStatus.AVAILABLE))
				.extracting(Seat::getCode)
				.containsExactly("A1");
	}

	@Test
	void shouldRejectDuplicateSeatCodeIgnoringCaseWithinSameSector() {
		Event event = eventRepository.saveAndFlush(new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00")));
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "VIP", new BigDecimal("250.00")));
		seatRepository.saveAndFlush(new Seat(sector, "A1"));

		assertThatThrownBy(() -> seatRepository.saveAndFlush(new Seat(sector, "a1")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
