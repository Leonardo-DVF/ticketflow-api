package com.leonardo.ticketflow.auth.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AdminBootstrapService adminBootstrapService;

	@Test
	void shouldCreateAnAdminWhenTheConfiguredEmailDoesNotExist() {
		AdminBootstrapProperties properties = new AdminBootstrapProperties(
				true, "  TicketFlow Admin  ", " Admin@Example.COM ", "admin-password");
		when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("admin-password")).thenReturn("hashed-password");

		adminBootstrapService.bootstrap(properties);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).findByEmailIgnoreCase("admin@example.com");
		verify(userRepository).save(userCaptor.capture());
		verify(passwordEncoder).encode("admin-password");

		User admin = userCaptor.getValue();
		assertThat(admin.getName()).isEqualTo("TicketFlow Admin");
		assertThat(admin.getEmail()).isEqualTo("admin@example.com");
		assertThat(admin.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void shouldPromoteAnExistingUserWithoutChangingItsPassword() {
		User existingUser = new User("Admin", "admin@example.com", "existing-hash", Role.USER);
		AdminBootstrapProperties properties = new AdminBootstrapProperties(
				true, "Ignored", "admin@example.com", "ignored-password");
		when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existingUser));

		adminBootstrapService.bootstrap(properties);

		assertThat(existingUser.getRole()).isEqualTo(Role.ADMIN);
		verify(userRepository).findByEmailIgnoreCase("admin@example.com");
		verifyNoMoreInteractions(userRepository);
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void shouldRejectIncompleteConfiguration() {
		AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "Admin", "", "password");

		assertThatThrownBy(() -> adminBootstrapService.bootstrap(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Admin bootstrap requires name, email, and password when enabled");

		verifyNoInteractions(userRepository, passwordEncoder);
	}
}
