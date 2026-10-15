package com.leonardo.ticketflow.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.reservation.entity.ReservationItem;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {

	List<ReservationItem> findAllByReservationId(Long reservationId);
}
