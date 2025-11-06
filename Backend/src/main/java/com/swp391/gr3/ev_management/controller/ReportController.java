package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.CreateReportRequest;
import com.swp391.gr3.ev_management.DTO.response.ReportResponse;
import com.swp391.gr3.ev_management.service.ReportService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/incidents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Staff Incident", description = "APIs for staff to manage incident reports")
public class ReportController {

    private final ReportService reportService;
    private final TokenService tokenService;

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/create")
    @Operation(
            summary = "Create a new incident",
            description = "Create a new incident report by a station staff"
    )
    public ResponseEntity<ReportResponse> createIncident(
            @Parameter(hidden = true) HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Incident creation request", required = true
            )
            @Valid @RequestBody CreateReportRequest body
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        ReportResponse response = reportService.createIncident(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        reportService.updateIncidentStatus(incidentId, status);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping
    @Operation(
            summary = "Get all incidents",
            description = "Get list of all incidents (admin/staff tool)"
    )
    public ResponseEntity<List<ReportResponse>> getIncidents() {
        List<ReportResponse> incidents = reportService.findAll();
        return ResponseEntity.ok(incidents);
    }

}
