package com.leonardo.ticketflow.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.lock.RedisSeatLockService;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.reservation.repository.ReservationItemRepository;
import com.leonardo.ticketflow.reservation.service.ReservationService;
import com.leonardo.ticketflow.reservation.service.ReservationExpirationService;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.repository.SectorRepository;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Testcontainers
@SpringBootTest(properties =
		"security.jwt.secret=ticketflow-concurrency-test-secret-key-32-bytes")
@Import(ReservationConcurrencyIntegrationTest.TestClockConfig.class)
class ReservationConcurrencyIntegrationTest {

	private static final Instant TEST_NOW = Instant.parse("2030-01-01T12:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SectorRepository sectorRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ReservationItemRepository reservationItemRepository;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ReservationExpirationService expirationService;

	@Autowired
	private RedisSeatLockService seatLockService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void cleanDatabase() {
		clock.setInstant(TEST_NOW);
		redisTemplate.delete(redisTemplate.keys("seat:lock:*"));
		reservationItemRepository.deleteAll();
		reservationRepository.deleteAll();
		seatRepository.deleteAll();
		sectorRepository.deleteAll();
		eventRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldExpireReservationAndReleaseSeatAfterTenMinutes() {
		User user = userRepository.saveAndFlush(new User(
				"Expiration User",
				"expiration@example.com",
				"encoded-password",
				Role.USER));
		Event event = new Event(
				"Muse - Rio de Janeiro",
				null,
				"Engenhao",
				OffsetDateTime.parse("2030-02-10T20:00:00-03:00"));
		event.changeStatus(EventStatus.AVAILABLE);
		eventRepository.saveAndFlush(event);
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "Arquibancada", new BigDecimal("150.00")));
		Seat seat = seatRepository.saveAndFlush(new Seat(sector, "B1"));
		var reservation = reservationService.create(
				user.getEmail(),
				new CreateReservationRequest(event.getId(), List.of(seat.getId())));
		assertThat(seatLockService.acquire(
				event.getId(),
				seat.getId(),
				"probe"))
				.isFalse();
		clock.setInstant(TEST_NOW.plusSeconds(11 * 60));

		int expiredCount = expirationService.expireDueReservations();

		assertThat(expiredCount).isEqualTo(1);
		assertThat(reservationRepository.findById(reservation.id()).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.EXPIRED);
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
				.isEqualTo(SeatStatus.AVAILABLE);
		assertThat(seatLockService.acquire(
				event.getId(),
				seat.getId(),
				"probe"))
				.isTrue();
	}

	@Test
	void shouldAllowOnlyOneConcurrentReservationForSameSeat() throws Exception {
		User firstUser = userRepository.saveAndFlush(new User(
				"First User",
				"first@example.com",
				"encoded-password",
				Role.USER));
		User secondUser = userRepository.saveAndFlush(new User(
				"Second User",
				"second@example.com",
				"encoded-password",
				Role.USER));
		Event event = new Event(
				"Coldplay - Brasilia",
				null,
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2026-12-20T20:00:00-03:00"));
		event.changeStatus(EventStatus.AVAILABLE);
		eventRepository.saveAndFlush(event);
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "VIP", new BigDecimal("250.00")));
		Seat seat = seatRepository.saveAndFlush(new Seat(sector, "A1"));
		CreateReservationRequest request = new CreateReservationRequest(
				event.getId(),
				List.of(seat.getId()));

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Attempt> firstAttempt = executor.submit(
					() -> attemptReservation(firstUser.getEmail(), request, ready, start));
			Future<Attempt> secondAttempt = executor.submit(
					() -> attemptReservation(secondUser.getEmail(), request, ready, start));

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			List<Attempt> attempts = List.of(
					firstAttempt.get(10, TimeUnit.SECONDS),
					secondAttempt.get(10, TimeUnit.SECONDS));

			assertThat(attempts).filteredOn(Attempt::success).hasSize(1);
			assertThat(attempts)
					.filteredOn(attempt -> attempt.error() instanceof SeatUnavailableException)
					.hasSize(1);
			assertThat(reservationRepository.count()).isEqualTo(1);
			assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
					.isEqualTo(SeatStatus.RESERVED);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void shouldPersistQueriesAndCancellation() {
		User user = userRepository.saveAndFlush(new User(
				"Cancellation User",
				"cancellation@example.com",
				"encoded-password",
				Role.USER));
		Event event = new Event(
				"The Killers - Sao Paulo",
				null,
				"Allianz Parque",
				OffsetDateTime.parse("2027-01-10T20:00:00-03:00"));
		event.changeStatus(EventStatus.AVAILABLE);
		eventRepository.saveAndFlush(event);
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "Pista", new BigDecimal("180.00")));
		Seat seat = seatRepository.saveAndFlush(new Seat(sector, "P1"));

		ReservationResponse created = reservationService.create(
				user.getEmail(),
				new CreateReservationRequest(event.getId(), List.of(seat.getId())));

		ReservationResponse found = reservationService.findById(
				user.getEmail(),
				created.id());
		assertThat(found.id()).isEqualTo(created.id());
		assertThat(reservationService.findMine(user.getEmail()))
				.extracting(ReservationResponse::id)
				.contains(created.id());

		reservationService.cancel(user.getEmail(), created.id());

		assertThat(reservationRepository.findById(created.id()).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CANCELLED);
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
				.isEqualTo(SeatStatus.AVAILABLE);
		assertThat(seatLockService.acquire(
				event.getId(),
				seat.getId(),
				"probe"))
				.isTrue();
	}

	private Attempt attemptReservation(
			String email,
			CreateReservationRequest request,
			CountDownLatch ready,
			CountDownLatch start) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			reservationService.create(email, request);
			return new Attempt(true, null);
		} catch (RuntimeException exception) {
			return new Attempt(false, exception);
		}
	}

	private record Attempt(boolean success, Throwable error) {
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfig {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(TEST_NOW);
		}
	}

	static class MutableClock extends Clock {

		private final AtomicReference<Instant> instant;

		MutableClock(Instant initialInstant) {
			this.instant = new AtomicReference<>(initialInstant);
		}

		void setInstant(Instant newInstant) {
			instant.set(newInstant);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(instant(), zone);
		}

		@Override
		public Instant instant() {
			return instant.get();
		}
	}
}
