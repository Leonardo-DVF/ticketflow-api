package com.leonardo.ticketflow.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.event.entity.Event;
import com.leonardo.ticketflow.event.entity.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findAllByStatusOrderByEventDateAsc(EventStatus status);

	Optional<Event> findByIdAndStatus(Long id, EventStatus status);
}
