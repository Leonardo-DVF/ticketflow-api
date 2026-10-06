package com.leonardo.ticketflow.auth.dto;

import java.time.Instant;

import com.leonardo.ticketflow.user.entity.Role;

public record UserResponse(
		Long id,
		String name,
		String email,
		Role role,
		Instant createdAt) {
}
