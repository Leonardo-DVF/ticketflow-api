package com.leonardo.ticketflow.auth.security;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.leonardo.ticketflow.auth.config.JwtConfig;
import com.leonardo.ticketflow.config.SecurityConfig;

@WebMvcTest(ProtectedTestController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
@TestPropertySource(properties =
		"security.jwt.secret=ticketflow-jwt-authentication-test-secret-32-bytes")
class JwtAuthenticationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void shouldAuthenticateRequestWithValidBearerToken() throws Exception {
		String token = createToken("leonardo@example.com", "USER", "https://ticketflow.local");

		mockMvc.perform(get("/test/protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("leonardo@example.com"))
				.andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
	}

	@Test
	void shouldRejectTokenFromAnotherIssuer() throws Exception {
		String token = createToken("leonardo@example.com", "USER", "https://another-issuer.local");

		mockMvc.perform(get("/test/protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication is required"));
	}

	private String createToken(String subject, String role, String issuer) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(subject)
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(3600))
				.claim("role", role)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
