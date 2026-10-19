package com.leonardo.ticketflow.auth.bootstrap;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Service
public class AdminBootstrapService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void bootstrap(AdminBootstrapProperties properties) {
		validate(properties);
		String email = properties.email().trim().toLowerCase(Locale.ROOT);

		userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
				User::promoteToAdmin,
				() -> createAdmin(properties, email));
	}

	private void createAdmin(AdminBootstrapProperties properties, String email) {
		User admin = new User(
				properties.name().trim(),
				email,
				passwordEncoder.encode(properties.password()),
				Role.ADMIN);
		userRepository.save(admin);
	}

	private void validate(AdminBootstrapProperties properties) {
		if (!StringUtils.hasText(properties.name())
				|| !StringUtils.hasText(properties.email())
				|| !StringUtils.hasText(properties.password())) {
			throw new IllegalStateException(
					"Admin bootstrap requires name, email, and password when enabled");
		}
	}
}
