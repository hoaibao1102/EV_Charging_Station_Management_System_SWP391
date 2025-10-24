package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.StopPointRequest;
import com.swp391.gr3.ev_management.DTO.response.StopPointResponse;
import com.swp391.gr3.ev_management.service.ChargingPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging-points")
@RequiredArgsConstructor
@Tag(name = "Staff Charging Point", description = "APIs for staff to manage charging points")
public class ChargingPointController {

    @Autowired
    private final ChargingPointService pointService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PostMapping("/stop")
    @Operation(summary = "Stop charging point", description = "Staff stops a charging point for maintenance or other reasons")
    public ResponseEntity<StopPointResponse> stopChargingPoint(
            @Valid @RequestBody StopPointRequest request
    ) {
        StopPointResponse response = pointService.stopChargingPoint(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pointId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get charging point status", description = "Get detailed status of a charging point")
    public ResponseEntity<StopPointResponse> getPointStatus(
            @Parameter(description = "Charging Point ID") @PathVariable Long pointId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        StopPointResponse response = pointService.getPointStatus(pointId, staffId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all charging points", description = "Get list of all charging points at a station")
    public ResponseEntity<List<StopPointResponse>> getPointsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<StopPointResponse> points = pointService.getPointsByStation(stationId, staffId);
        return ResponseEntity.ok(points);
    }

}
