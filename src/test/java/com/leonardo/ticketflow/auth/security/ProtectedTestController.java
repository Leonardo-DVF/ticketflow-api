package com.leonardo.ticketflow.auth.security;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProtectedTestController {

	@GetMapping("/test/protected")
	Map<String, Object> protectedResource(Authentication authentication) {
		List<String> authorities = authentication.getAuthorities()
				.stream()
				.map(Object::toString)
				.toList();
		return Map.of("subject", authentication.getName(), "authorities", authorities);
	}
}
