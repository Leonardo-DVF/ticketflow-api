package com.leonardo.ticketflow.reservation.lock;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ticketflow.reservation.lock")
public record SeatLockProperties(Duration ttl) {

	public SeatLockProperties {
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw new IllegalArgumentException("Reservation lock TTL must be positive");
		}
	}
}
