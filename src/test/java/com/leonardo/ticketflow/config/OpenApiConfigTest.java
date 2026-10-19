package com.leonardo.ticketflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

	private final OpenApiConfig openApiConfig = new OpenApiConfig();

	@Test
	void shouldDescribeApiAndJwtBearerAuthentication() {
		OpenAPI openApi = openApiConfig.ticketFlowOpenApi();

		assertThat(openApi.getInfo().getTitle()).isEqualTo("TicketFlow API");
		assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
		SecurityScheme securityScheme = openApi.getComponents()
				.getSecuritySchemes()
				.get("bearerAuth");
		assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
		assertThat(securityScheme.getScheme()).isEqualTo("bearer");
		assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
	}
}
