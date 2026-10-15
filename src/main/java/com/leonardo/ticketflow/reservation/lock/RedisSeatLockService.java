package com.leonardo.ticketflow.reservation.lock;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisSeatLockService {

	private static final String KEY_PREFIX = "seat:lock:";
	private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
			"""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			    return redis.call('del', KEYS[1])
			end
			return 0
			""",
			Long.class);

	private final StringRedisTemplate redisTemplate;
	private final SeatLockProperties properties;

	public RedisSeatLockService(
			StringRedisTemplate redisTemplate,
			SeatLockProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public boolean acquire(Long eventId, Long seatId, String lockOwner) {
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
				key(eventId, seatId),
				lockOwner,
				properties.ttl());
		return Boolean.TRUE.equals(acquired);
	}

	public boolean release(Long eventId, Long seatId, String lockOwner) {
		Long released = redisTemplate.execute(
				RELEASE_SCRIPT,
				List.of(key(eventId, seatId)),
				lockOwner);
		return Long.valueOf(1L).equals(released);
	}

	String key(Long eventId, Long seatId) {
		return KEY_PREFIX + eventId + ":" + seatId;
	}
}
