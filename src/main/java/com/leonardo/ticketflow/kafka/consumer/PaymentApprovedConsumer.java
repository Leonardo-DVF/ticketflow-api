package com.leonardo.ticketflow.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.leonardo.ticketflow.kafka.config.KafkaConsumerGroups;
import com.leonardo.ticketflow.kafka.config.KafkaTopics;
import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;
import com.leonardo.ticketflow.reservation.service.ReservationConfirmationService;

@Component
public class PaymentApprovedConsumer {

	private final ReservationConfirmationService confirmationService;

	public PaymentApprovedConsumer(ReservationConfirmationService confirmationService) {
		this.confirmationService = confirmationService;
	}

	@KafkaListener(
			topics = KafkaTopics.PAYMENT_APPROVED,
			groupId = KafkaConsumerGroups.TICKET)
	public void consume(PaymentApprovedEvent event) {
		confirmationService.confirm(event);
	}
}
