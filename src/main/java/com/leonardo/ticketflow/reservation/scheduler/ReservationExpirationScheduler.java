package com.leonardo.ticketflow.reservation.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.reservation.service.ReservationExpirationService;

@Component
public class ReservationExpirationScheduler {

	private final ReservationExpirationService expirationService;

	public ReservationExpirationScheduler(ReservationExpirationService expirationService) {
		this.expirationService = expirationService;
	}

	@Scheduled(
			initialDelayString = "${ticketflow.reservation.expiration-interval-ms:60000}",
			fixedDelayString = "${ticketflow.reservation.expiration-interval-ms:60000}")
	public void expireReservations() {
		expirationService.expireDueReservations();
	}
}
