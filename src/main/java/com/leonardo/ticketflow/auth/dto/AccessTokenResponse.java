package com.leonardo.ticketflow.auth.dto;

public record AccessTokenResponse(
		String token,
		String type,
		long expiresIn) {
}
