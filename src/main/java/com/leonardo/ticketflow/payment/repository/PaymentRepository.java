package com.leonardo.ticketflow.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.payment.entity.Payment;
import com.leonardo.ticketflow.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	boolean existsByReservationIdAndStatus(Long reservationId, PaymentStatus status);
}
