package com.leonardo.ticketflow.reservation.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisSeatLockIntegrationTest {

	private static final int REDIS_PORT = 6379;

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
			.withExposedPorts(REDIS_PORT);

	@Test
	void shouldAcquireExpireAndSafelyReleaseLockInRedis() {
		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
				redis.getHost(),
				redis.getMappedPort(REDIS_PORT));
		connectionFactory.afterPropertiesSet();
		connectionFactory.start();
		try {
			StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
			redisTemplate.afterPropertiesSet();
			RedisSeatLockService lockService = new RedisSeatLockService(
					redisTemplate,
					new SeatLockProperties(Duration.ofMinutes(10)));

			assertThat(lockService.acquire(10L, 100L, "reservation:50")).isTrue();
			assertThat(lockService.acquire(10L, 100L, "reservation:51")).isFalse();
			assertThat(redisTemplate.getExpire(
					"seat:lock:10:100",
					TimeUnit.SECONDS))
					.isPositive()
					.isLessThanOrEqualTo(600L);
			assertThat(lockService.release(10L, 100L, "reservation:51")).isFalse();
			assertThat(redisTemplate.hasKey("seat:lock:10:100")).isTrue();
			assertThat(lockService.release(10L, 100L, "reservation:50")).isTrue();
			assertThat(redisTemplate.hasKey("seat:lock:10:100")).isFalse();
		} finally {
			connectionFactory.destroy();
		}
	}
}
