package com.leonardo.ticketflow.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.event.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
