package com.leonardo.ticketflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.leonardo.ticketflow.auth.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@InjectMocks
	private AuthenticationService authenticationService;

	@Test
	void shouldNormalizeEmailAndDelegateAuthentication() {
		LoginRequest request = new LoginRequest("  Leonardo@Example.COM  ", "plain-password");

		authenticationService.authenticate(request);

		ArgumentCaptor<UsernamePasswordAuthenticationToken> credentialsCaptor =
				ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(credentialsCaptor.capture());

		UsernamePasswordAuthenticationToken credentials = credentialsCaptor.getValue();
		assertThat(credentials.getPrincipal()).isEqualTo("leonardo@example.com");
		assertThat(credentials.getCredentials()).isEqualTo("plain-password");
		assertThat(credentials.isAuthenticated()).isFalse();
	}
}
