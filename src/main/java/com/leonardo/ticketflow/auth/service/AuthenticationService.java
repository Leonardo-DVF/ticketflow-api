package com.leonardo.ticketflow.auth.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.leonardo.ticketflow.auth.dto.LoginRequest;

@Service
public class AuthenticationService {

	private final AuthenticationManager authenticationManager;

	public AuthenticationService(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void authenticate(LoginRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
		UsernamePasswordAuthenticationToken credentials =
				UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password());

		authenticationManager.authenticate(credentials);
	}
}
