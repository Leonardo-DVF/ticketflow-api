package com.leonardo.ticketflow.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.leonardo.ticketflow.auth.dto.UserRegistrationRequest;
import com.leonardo.ticketflow.auth.dto.UserResponse;
import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.exception.EmailAlreadyRegisteredException;
import com.leonardo.ticketflow.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserRegistrationService registrationService;

	@Test
	void shouldNormalizeEmailAndHashPasswordWhenRegistering() {
		UserRegistrationRequest request = new UserRegistrationRequest(
				"  Leonardo  ",
				"  Leonardo@Example.COM  ",
				"plain-password");
		when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = registrationService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).existsByEmailIgnoreCase("leonardo@example.com");
		verify(userRepository).saveAndFlush(userCaptor.capture());
		verify(passwordEncoder).encode("plain-password");

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getName()).isEqualTo("Leonardo");
		assertThat(savedUser.getEmail()).isEqualTo("leonardo@example.com");
		assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(savedUser.getRole()).isEqualTo(Role.USER);
		assertThat(response.email()).isEqualTo("leonardo@example.com");
		assertThat(response.role()).isEqualTo(Role.USER);
	}

	@Test
	void shouldRejectAnEmailThatIsAlreadyRegistered() {
		UserRegistrationRequest request = new UserRegistrationRequest(
				"Leonardo",
				"leonardo@example.com",
				"plain-password");
		when(userRepository.existsByEmailIgnoreCase("leonardo@example.com")).thenReturn(true);

		assertThatThrownBy(() -> registrationService.register(request))
				.isInstanceOf(EmailAlreadyRegisteredException.class)
				.hasMessageContaining("leonardo@example.com");

		verify(userRepository, never()).saveAndFlush(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void shouldHandleConcurrentRegistrationsForTheSameEmail() {
		UserRegistrationRequest request = new UserRegistrationRequest(
				"Leonardo",
				"leonardo@example.com",
				"plain-password");
		when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate email"));

		assertThatThrownBy(() -> registrationService.register(request))
				.isInstanceOf(EmailAlreadyRegisteredException.class)
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
	}
}
