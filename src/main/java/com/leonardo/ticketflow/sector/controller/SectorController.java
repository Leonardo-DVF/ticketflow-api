package com.leonardo.ticketflow.sector.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leonardo.ticketflow.sector.dto.CreateSectorRequest;
import com.leonardo.ticketflow.sector.dto.SectorResponse;
import com.leonardo.ticketflow.sector.service.SectorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events/{eventId}/sectors")
public class SectorController {

	private final SectorService sectorService;

	public SectorController(SectorService sectorService) {
		this.sectorService = sectorService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SectorResponse create(
			@PathVariable Long eventId,
			@Valid @RequestBody CreateSectorRequest request) {
		return sectorService.create(eventId, request);
	}

	@GetMapping
	public List<SectorResponse> findByEvent(@PathVariable Long eventId) {
		return sectorService.findByEvent(eventId);
	}
}
