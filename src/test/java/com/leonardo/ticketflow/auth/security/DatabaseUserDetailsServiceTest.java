package com.leonardo.ticketflow.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private DatabaseUserDetailsService userDetailsService;

	@Test
	void shouldLoadUserFromDatabase() {
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"hashed-password",
				Role.ADMIN);
		when(userRepository.findByEmailIgnoreCase("leonardo@example.com"))
				.thenReturn(Optional.of(user));

		UserDetails userDetails = userDetailsService.loadUserByUsername("  Leonardo@Example.COM  ");

		assertThat(userDetails.getUsername()).isEqualTo("leonardo@example.com");
		assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
		assertThat(userDetails.getAuthorities())
				.extracting(Object::toString)
				.containsExactly("ROLE_ADMIN");
		verify(userRepository).findByEmailIgnoreCase("leonardo@example.com");
	}

	@Test
	void shouldRejectUnknownUser() {
		when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("User not found");
	}
}
