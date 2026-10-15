package com.leonardo.ticketflow.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.exception.SeatUnavailableException;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.reservation.service.ReservationService;
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
class ReservationConcurrencyIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

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
	private ReservationService reservationService;

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
}
