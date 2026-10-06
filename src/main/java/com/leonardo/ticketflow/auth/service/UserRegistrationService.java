package com.leonardo.ticketflow.auth.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leonardo.ticketflow.auth.dto.UserRegistrationRequest;
import com.leonardo.ticketflow.auth.dto.UserResponse;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.exception.EmailAlreadyRegisteredException;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Service
public class UserRegistrationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse register(UserRegistrationRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new EmailAlreadyRegisteredException(normalizedEmail);
		}

		User user = new User(
				request.name().trim(),
				normalizedEmail,
				passwordEncoder.encode(request.password()),
				Role.USER);

		try {
			User savedUser = userRepository.saveAndFlush(user);
			return toResponse(savedUser);
		} catch (DataIntegrityViolationException exception) {
			throw new EmailAlreadyRegisteredException(normalizedEmail, exception);
		}
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt());
	}
}
