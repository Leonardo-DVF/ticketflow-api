package com.leonardo.ticketflow.reservation.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisSeatLockServiceTest {

	private static final Duration TTL = Duration.ofMinutes(10);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisSeatLockService lockService;

	@BeforeEach
	void setUp() {
		lockService = new RedisSeatLockService(
				redisTemplate,
				new SeatLockProperties(TTL));
	}

	@Test
	void shouldAcquireLockWithExpectedKeyOwnerAndTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(
				"seat:lock:10:100",
				"reservation:50",
				TTL))
				.thenReturn(true);

		boolean acquired = lockService.acquire(10L, 100L, "reservation:50");

		assertThat(acquired).isTrue();
		verify(valueOperations).setIfAbsent(
				"seat:lock:10:100",
				"reservation:50",
				TTL);
	}

	@Test
	void shouldReportLockAsUnavailableWhenKeyAlreadyExists() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(
				"seat:lock:10:100",
				"reservation:51",
				TTL))
				.thenReturn(false);

		boolean acquired = lockService.acquire(10L, 100L, "reservation:51");

		assertThat(acquired).isFalse();
	}

	@Test
	void shouldReleaseLockWhenOwnerMatches() {
		when(redisTemplate.execute(
				org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
				eq(List.of("seat:lock:10:100")),
				eq("reservation:50")))
				.thenReturn(1L);

		boolean released = lockService.release(10L, 100L, "reservation:50");

		assertThat(released).isTrue();
	}

	@Test
	void shouldNotReleaseLockOwnedByAnotherReservation() {
		when(redisTemplate.execute(
				org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
				eq(List.of("seat:lock:10:100")),
				eq("reservation:50")))
				.thenReturn(0L);

		boolean released = lockService.release(10L, 100L, "reservation:50");

		assertThat(released).isFalse();
	}

	@Test
	void shouldTransferLockToReservationOwner() {
		when(redisTemplate.execute(
				org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
				eq(List.of("seat:lock:10:100")),
				eq("pending:token"),
				eq("reservation:50")))
				.thenReturn(1L);

		boolean transferred = lockService.transfer(
				10L,
				100L,
				"pending:token",
				"reservation:50");

		assertThat(transferred).isTrue();
	}

	@Test
	void shouldNotTransferLockOwnedByAnotherToken() {
		when(redisTemplate.execute(
				org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
				eq(List.of("seat:lock:10:100")),
				eq("pending:wrong-token"),
				eq("reservation:50")))
				.thenReturn(0L);

		boolean transferred = lockService.transfer(
				10L,
				100L,
				"pending:wrong-token",
				"reservation:50");

		assertThat(transferred).isFalse();
	}
}
