package com.leonardo.ticketflow.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.auth.dto.UserRegistrationRequest;
import com.leonardo.ticketflow.auth.dto.UserResponse;
import com.leonardo.ticketflow.auth.service.UserRegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final UserRegistrationService registrationService;

	public AuthController(UserRegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {
		return registrationService.register(request);
	}
}
