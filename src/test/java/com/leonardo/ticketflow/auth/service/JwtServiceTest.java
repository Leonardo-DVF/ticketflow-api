package com.leonardo.ticketflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.leonardo.ticketflow.auth.config.JwtProperties;
import com.leonardo.ticketflow.auth.dto.AccessTokenResponse;

class JwtServiceTest {

	private static final String SECRET = "ticketflow-unit-test-secret-key-at-least-32-bytes";

	@Test
	void shouldGenerateSignedTokenWithExpectedClaims() {
		SecretKey secretKey = new SecretKeySpec(
				SECRET.getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(secretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
		JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		JwtProperties properties = new JwtProperties(SECRET, "https://ticketflow.local", 3600);
		JwtService jwtService = new JwtService(encoder, properties, Clock.systemUTC());
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				"leonardo@example.com",
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));

		AccessTokenResponse response = jwtService.generateToken(authentication);
		Jwt jwt = decoder.decode(response.token());

		assertThat(response.type()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600);
		assertThat(jwt.getIssuer().toString()).isEqualTo("https://ticketflow.local");
		assertThat(jwt.getSubject()).isEqualTo("leonardo@example.com");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).hasSeconds(3600);
	}
}
