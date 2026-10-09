package com.leonardo.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.leonardo.ticketflow.user.entity.Role;
import com.leonardo.ticketflow.user.entity.User;
import com.leonardo.ticketflow.user.repository.UserRepository;

@Testcontainers
@SpringBootTest(properties =
		"security.jwt.secret=ticketflow-integration-test-secret-key-32-bytes")
@Transactional
class PersistenceIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private UserRepository userRepository;

	@Test
	void shouldApplyUserTableMigration() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'users'
				""")
				.query(Integer.class)
				.single();

		assertThat(tableCount).isEqualTo(1);
	}

	@Test
	void shouldPersistAndFindUserByEmailIgnoringCase() {
		User user = new User(
				"Leonardo",
				"leonardo@example.com",
				"encoded-password",
				Role.USER);

		User savedUser = userRepository.saveAndFlush(user);

		assertThat(savedUser.getId()).isNotNull();
		assertThat(savedUser.getCreatedAt()).isNotNull();
		assertThat(userRepository.findByEmailIgnoreCase("LEONARDO@EXAMPLE.COM"))
				.isPresent()
				.get()
				.extracting(User::getRole)
				.isEqualTo(Role.USER);
	}
}
