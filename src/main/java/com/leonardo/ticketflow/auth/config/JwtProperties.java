package com.leonardo.ticketflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
		@NotBlank String secret,
		@NotBlank String issuer,
		@Positive long expirationSeconds) {
}
