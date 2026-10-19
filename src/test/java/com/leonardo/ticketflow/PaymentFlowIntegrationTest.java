package com.leonardo.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.leonardo.ticketflow.audit.entity.AuditEventType;
import com.leonardo.ticketflow.audit.repository.AuditLogRepository;
import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;
import com.leonardo.ticketflow.event.repository.EventRepository;
import com.leonardo.ticketflow.payment.dto.PaymentResponse;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;
import com.leonardo.ticketflow.payment.service.PaymentService;
import com.leonardo.ticketflow.reservation.dto.CreateReservationRequest;
import com.leonardo.ticketflow.reservation.dto.ReservationResponse;
import com.leonardo.ticketflow.reservation.entity.ReservationStatus;
import com.leonardo.ticketflow.reservation.repository.ReservationRepository;
import com.leonardo.ticketflow.reservation.service.ReservationService;
import com.leonardo.ticketflow.seat.entity.Seat;
import com.leonardo.ticketflow.seat.entity.SeatStatus;
import com.leonardo.ticketflow.seat.repository.SeatRepository;
import com.leonardo.ticketflow.sector.entity.Sector;
import com.leonardo.ticketflow.sector.repository.SectorRepository;
import com.leonardo.ticketflow.ticket.repository.TicketRepository;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
		"security.jwt.secret=ticketflow-payment-flow-test-secret-key-32-bytes",
		"ticketflow.reservation.expiration-interval-ms=3600000",
		"ticketflow.kafka.topic-partitions=1"
})
class PaymentFlowIntegrationTest {

	private static final int REDIS_PORT = 6379;

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
			.withExposedPorts(REDIS_PORT);

	@Container
	static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.1");

	@DynamicPropertySource
	static void infrastructureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
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
	private TicketRepository ticketRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	void shouldConfirmReservationAndIssueTicketAfterPaymentApproval() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"Payment Flow User",
				"payment-flow@example.com",
				"encoded-password",
				Role.USER));
		Event event = new Event(
				"Coldplay - Brasilia",
				"End-to-end payment flow",
				"Arena BRB Mane Garrincha",
				OffsetDateTime.parse("2030-12-20T20:00:00-03:00"));
		event.changeStatus(EventStatus.AVAILABLE);
		eventRepository.saveAndFlush(event);
		Sector sector = sectorRepository.saveAndFlush(
				new Sector(event, "VIP", new BigDecimal("250.00")));
		Seat seat = seatRepository.saveAndFlush(new Seat(sector, "A10"));

		ReservationResponse reservation = reservationService.create(
				user.getEmail(),
				new CreateReservationRequest(event.getId(), List.of(seat.getId())));

		assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
				.isEqualTo(SeatStatus.RESERVED);
		assertThat(redisTemplate.hasKey(lockKey(event.getId(), seat.getId()))).isTrue();

		PaymentResponse payment = paymentService.approve(user.getEmail(), reservation.id());

		assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
		awaitCompletedFlow(reservation.id(), payment.id(), seat.getId(), event.getId());

		assertThat(reservationRepository.findById(reservation.id()).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
				.isEqualTo(SeatStatus.SOLD);
		assertThat(ticketRepository.findAllByReservationIdOrderById(reservation.id()))
				.singleElement()
				.satisfies(ticket -> {
					assertThat(ticket.getSeat().getId()).isEqualTo(seat.getId());
					assertThat(ticket.getCode()).isNotNull();
				});
		assertThat(auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_APPROVED,
				payment.id()))
				.isTrue();
		assertThat(redisTemplate.hasKey(lockKey(event.getId(), seat.getId()))).isFalse();
	}

	private void awaitCompletedFlow(
			Long reservationId,
			Long paymentId,
			Long seatId,
			Long eventId) throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
		while (Instant.now().isBefore(deadline)) {
			if (flowCompleted(reservationId, paymentId, seatId, eventId)) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Payment approval was not fully processed within 15 seconds");
	}

	private boolean flowCompleted(
			Long reservationId,
			Long paymentId,
			Long seatId,
			Long eventId) {
		boolean reservationConfirmed = reservationRepository.findById(reservationId)
				.map(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
				.orElse(false);
		boolean seatSold = seatRepository.findById(seatId)
				.map(seat -> seat.getStatus() == SeatStatus.SOLD)
				.orElse(false);
		boolean ticketIssued = ticketRepository
				.findAllByReservationIdOrderById(reservationId)
				.size() == 1;
		boolean auditRecorded = auditLogRepository.existsByEventTypeAndAggregateId(
				AuditEventType.PAYMENT_APPROVED,
				paymentId);
		boolean lockReleased = Boolean.FALSE.equals(redisTemplate.hasKey(lockKey(eventId, seatId)));
		return reservationConfirmed
				&& seatSold
				&& ticketIssued
				&& auditRecorded
				&& lockReleased;
	}

	private String lockKey(Long eventId, Long seatId) {
		return "seat:lock:" + eventId + ":" + seatId;
	}
}
