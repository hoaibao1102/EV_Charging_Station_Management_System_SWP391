package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;
import com.swp391.gr3.ev_management.service.StaffIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/incidents")
@RequiredArgsConstructor
@Tag(name = "Staff Incident", description = "APIs for staff to manage incident reports")
public class IncidentController {

    @Autowired
    private final StaffIncidentService incidentService;

    @GetMapping
    @Operation(summary = "Get all incidents", description = "Get list of all incidents at a station")
    public ResponseEntity<List<IncidentResponse>> getIncidentsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<IncidentResponse> incidents = incidentService.getIncidentsByStation(stationId, staffId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/unresolved")
    @Operation(summary = "Get unresolved incidents", description = "Get list of unresolved incidents at a station")
    public ResponseEntity<List<IncidentResponse>> getUnresolvedIncidents(
            @Parameter(description = "Station ID") @RequestParam Long stationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<IncidentResponse> incidents = incidentService.getUnresolvedIncidentsByStation(stationId, staffId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get incident detail", description = "Get detailed information of an incident")
    public ResponseEntity<IncidentResponse> getIncidentById(
            @Parameter(description = "Incident ID") @PathVariable Long incidentId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        IncidentResponse response = incidentService.getIncidentById(incidentId, staffId);
        return ResponseEntity.ok(response);
    }
}
