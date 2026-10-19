package com.leonardo.ticketflow.auth.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketflow.admin-bootstrap")
public record AdminBootstrapProperties(
		boolean enabled,
		String name,
		String email,
		String password) {
}
