package com.leonardo.ticketflow.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.leonardo.ticketflow.auth.config.JwtProperties;
import com.leonardo.ticketflow.auth.dto.AccessTokenResponse;

@Service
public class JwtService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final String ROLE_PREFIX = "ROLE_";
	private static final String TOKEN_ISSUER = "https://ticketflow.local";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public AccessTokenResponse generateToken(Authentication authentication) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(properties.expirationSeconds());
		String role = authentication.getAuthorities()
				.stream()
				.map(Object::toString)
				.filter(authority -> authority.startsWith(ROLE_PREFIX))
				.map(authority -> authority.substring(ROLE_PREFIX.length()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(TOKEN_ISSUER)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(authentication.getName())
				.claim("role", role)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
				.type("JWT")
				.build();

		String token = jwtEncoder
				.encode(JwtEncoderParameters.from(header, claims))
				.getTokenValue();

		return new AccessTokenResponse(token, TOKEN_TYPE, properties.expirationSeconds());
	}
}
