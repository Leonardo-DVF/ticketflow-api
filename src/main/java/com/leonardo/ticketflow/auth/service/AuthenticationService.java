package com.leonardo.ticketflow.auth.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.leonardo.ticketflow.auth.dto.AccessTokenResponse;
import com.leonardo.ticketflow.auth.dto.LoginRequest;

@Service
public class AuthenticationService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public AccessTokenResponse authenticate(LoginRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
		UsernamePasswordAuthenticationToken credentials =
				UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password());

		Authentication authentication = authenticationManager.authenticate(credentials);
		return jwtService.generateToken(authentication);
	}
}
