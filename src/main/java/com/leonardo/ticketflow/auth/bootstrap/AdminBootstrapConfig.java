package com.leonardo.ticketflow.auth.bootstrap;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapConfig {

	@Bean
	@ConditionalOnProperty(
			prefix = "ticketflow.admin-bootstrap",
			name = "enabled",
			havingValue = "true")
	ApplicationRunner adminBootstrapRunner(
			AdminBootstrapService adminBootstrapService,
			AdminBootstrapProperties properties) {
		return arguments -> adminBootstrapService.bootstrap(properties);
	}
}
