package com.leonardo.ticketflow.reservation.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.leonardo.ticketflow.reservation.service.ReservationExpirationService;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationSchedulerTest {

	@Mock
	private ReservationExpirationService expirationService;

	@InjectMocks
	private ReservationExpirationScheduler scheduler;

	@Test
	void shouldDelegateExpirationProcessing() {
		scheduler.expireReservations();

		verify(expirationService).expireDueReservations();
	}
}
