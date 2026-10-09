package com.leonardo.ticketflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.leonardo.ticketflow.auth.dto.AccessTokenResponse;
import com.leonardo.ticketflow.auth.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthenticationService authenticationService;

	@Test
	void shouldNormalizeEmailAndDelegateAuthentication() {
		LoginRequest request = new LoginRequest("  Leonardo@Example.COM  ", "plain-password");
		Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(
				"leonardo@example.com",
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		AccessTokenResponse expectedResponse =
				new AccessTokenResponse("signed.jwt.token", "Bearer", 3600);
		when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedUser);
		when(jwtService.generateToken(authenticatedUser)).thenReturn(expectedResponse);

		AccessTokenResponse response = authenticationService.authenticate(request);

		ArgumentCaptor<UsernamePasswordAuthenticationToken> credentialsCaptor =
				ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(credentialsCaptor.capture());

		UsernamePasswordAuthenticationToken credentials = credentialsCaptor.getValue();
		assertThat(credentials.getPrincipal()).isEqualTo("leonardo@example.com");
		assertThat(credentials.getCredentials()).isEqualTo("plain-password");
		assertThat(credentials.isAuthenticated()).isFalse();
		assertThat(response).isEqualTo(expectedResponse);
		verify(jwtService).generateToken(authenticatedUser);
	}
}
