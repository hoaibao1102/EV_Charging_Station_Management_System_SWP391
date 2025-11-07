package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.*;
import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.service.ChargingPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging-points")
@RequiredArgsConstructor
@Tag(name = "Staff Charging Point", description = "APIs for staff to manage charging points")
public class ChargingPointController {

    private final ChargingPointService pointService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/create")
    @Operation(summary = "Create a new point", description = "Endpoint to create a new charging point")
    public ResponseEntity<ChargingPointResponse> createPoint(@RequestBody CreateChargingPointRequest request) {
        try {
            ChargingPointResponse response = pointService.createChargingPoint(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PostMapping("/stop")
    @Operation(summary = "Stop charging point", description = "Staff stops a charging point for maintenance or other reasons")
    public ResponseEntity<ChargingPointResponse> stopChargingPoint(
            @Valid @RequestBody StopChargingPointRequest request
    ) {
        ChargingPointResponse response = pointService.stopChargingPoint(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all charging points", description = "Get list of all charging points at a station")
    public ResponseEntity<List<ChargingPointResponse>> getAllPoints(){
        return ResponseEntity.ok(pointService.getAllPoints());
    }

    @GetMapping("station/{stationId}")
    @Operation(summary = "Get charging points by station", description = "Get all charging points for a specific station")
    public ResponseEntity<List<ChargingPointResponse>> getPointsByStation(
            @Parameter(description = "Charging Station ID") @PathVariable Long stationId
    ) {
        List<ChargingPointResponse> responses = pointService.getPointsByStationId(stationId);
        return ResponseEntity.ok(responses);
    }

}
