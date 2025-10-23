package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;
import com.swp391.gr3.ev_management.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/incidents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Staff Incident", description = "APIs for staff to manage incident reports")
public class IncidentController {

    @Autowired
    private final IncidentService incidentService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(
            summary = "Get all incidents",
            description = "Get list of all incidents (admin/staff tool)"
    )
    public ResponseEntity<List<IncidentResponse>> getIncidents() {
        List<IncidentResponse> incidents = incidentService.findAll();
        return ResponseEntity.ok(incidents);
    }

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/create")
    @Operation(
            summary = "Create a new incident",
            description = "Create a new incident report by a station staff"
    )
    public ResponseEntity<IncidentResponse> createIncident(
            @Parameter(description = "Incident creation request") @RequestBody com.swp391.gr3.ev_management.DTO.request.CreateIncidentRequest request
    ) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{incidentId}/status")
    @Operation(
            summary = "Update incident status",
            description = "Update the status of an incident report"
    )
    public ResponseEntity<Void> updateIncidentStatus(
            @Parameter(description = "Incident ID") @PathVariable Long incidentId,
            @Parameter(description = "New status") @RequestParam String status
    ) {
        incidentService.updateIncidentStatus(incidentId, status);
        return ResponseEntity.ok().build();
    }

}
