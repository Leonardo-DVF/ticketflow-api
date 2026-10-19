package com.leonardo.ticketflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	@Bean
	OpenAPI ticketFlowOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("TicketFlow API")
						.version("v1")
						.description("Event management, seat reservations, simulated payments, and ticket issuing."))
				.components(new Components().addSecuritySchemes(
						BEARER_AUTH_SCHEME,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
