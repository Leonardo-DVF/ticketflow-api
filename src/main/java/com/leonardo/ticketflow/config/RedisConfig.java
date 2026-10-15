package com.leonardo.ticketflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.leonardo.ticketflow.reservation.lock.SeatLockProperties;

@Configuration
@EnableConfigurationProperties(SeatLockProperties.class)
public class RedisConfig {
}
