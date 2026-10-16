package com.leonardo.ticketflow.kafka.consumer;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.reservation.service.ReservationConfirmationService;

@ExtendWith(MockitoExtension.class)
class PaymentApprovedConsumerTest {

	@Mock
	private ReservationConfirmationService confirmationService;

	@InjectMocks
	private PaymentApprovedConsumer consumer;

	@Test
	void shouldDelegateApprovedPaymentForConfirmation() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
				80L,
				50L,
				1L,
				10L,
				new BigDecimal("250.00"),
				Instant.parse("2026-10-16T15:00:00Z"));

		consumer.consume(event);

		verify(confirmationService).confirm(event);
	}
}
