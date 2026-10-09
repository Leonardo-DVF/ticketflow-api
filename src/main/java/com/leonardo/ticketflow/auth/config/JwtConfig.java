package com.leonardo.ticketflow.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException("JWT secret must contain at least 32 bytes");
		}

		SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
		return NimbusJwtEncoder.withSecretKey(secretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	Clock jwtClock() {
		return Clock.systemUTC();
	}
}
