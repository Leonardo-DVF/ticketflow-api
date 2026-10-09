package com.leonardo.ticketflow.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.leonardo.ticketflow.event.exception.InvalidEventException;
import com.leonardo.ticketflow.user.exception.EmailAlreadyRegisteredException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(
			AuthenticationException exception,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.UNAUTHORIZED,
				"Invalid email or password",
				request.getRequestURI());
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	public ResponseEntity<ApiError> handleEmailAlreadyRegistered(
			EmailAlreadyRegisteredException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(InvalidEventException.class)
	public ResponseEntity<ApiError> handleInvalidEvent(
			InvalidEventException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, String path) {
		ApiError error = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path);

		return ResponseEntity.status(status).body(error);
	}
}
