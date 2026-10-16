package com.leonardo.ticketflow.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.leonardo.ticketflow.kafka.event.PaymentApprovedEvent;

@Service
public class NotificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

	public void sendPaymentApproved(PaymentApprovedEvent event) {
		LOGGER.info(
				"Notification sent to user {} for approved payment {} and reservation {}.",
				event.userId(),
				event.paymentId(),
				event.reservationId());
	}
}
