package com.leonardo.ticketflow.sector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leonardo.ticketflow.sector.entity.Sector;

public interface SectorRepository extends JpaRepository<Sector, Long> {

	List<Sector> findAllByEventIdOrderByNameAsc(Long eventId);

	boolean existsByEventIdAndNameIgnoreCase(Long eventId, String name);
}
