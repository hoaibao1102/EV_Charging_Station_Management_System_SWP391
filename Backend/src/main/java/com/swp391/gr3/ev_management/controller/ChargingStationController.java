package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.service.ChargingStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging-stations")
@RequiredArgsConstructor
@Tag(name = "Charging Station", description = "APIs for managing charging stations")
public class ChargingStationController {

    @Autowired
    private final ChargingStationService chargingStationService;

    // GET all
    @GetMapping
    @Operation(summary = "Get all charging stations")
    public ResponseEntity<List<ChargingStationResponse>> getAllStations() {
        return ResponseEntity.ok(chargingStationService.getAllStations());
    }

    // GET by id
    @GetMapping("/{id}")
    @Operation(summary = "Get charging station by ID")
    public ResponseEntity<ChargingStationResponse> getStationById(@PathVariable long id) {
        ChargingStationResponse response = chargingStationService.findByStationId(id);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    // POST create
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Add a new charging station")
    public ResponseEntity<ChargingStationResponse> addStation(@RequestBody ChargingStationRequest request) {
        ChargingStationResponse created = chargingStationService.addChargingStation(request);
        return ResponseEntity.ok(created);
    }

    // PUT update
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing charging station")
    public ResponseEntity<ChargingStationResponse> updateStation(
            @PathVariable long id,
            @RequestBody ChargingStationRequest request
    ) {
        ChargingStationResponse updated = chargingStationService.updateChargingStation(id, request);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }
}
