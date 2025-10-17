package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.ViewCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.service.StaffCharSessionService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/charging-sessions")
@RequiredArgsConstructor
@Tag(name = "Staff Charging Session", description = "APIs for staff to manage charging sessions")
public class ChargingSessionController {
    private final StaffCharSessionService staffCharSessionService;

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("all-session")
    public List<ChargingSessionResponse> getAllSession() {
        return staffCharSessionService.getAll()
                .stream()
                .map(session -> new ChargingSessionResponse(
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getEnergyKWh(),
                        session.getDurationMinutes(),
                        session.getCost(),
                        session.getStatus(),
                        session.getInvoice()
                ))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/start")
    @Operation(summary = "Start charging session", description = "Staff starts a new charging session for a confirmed booking")
    public ResponseEntity<StartCharSessionResponse> startChargingSession(
            @Valid @RequestBody StartCharSessionRequest request
    ) {
        StartCharSessionResponse response = staffCharSessionService.startChargingSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/stop")
    @Operation(summary = "Stop charging session", description = "Staff stops an ongoing charging session and records final energy")
    public ResponseEntity<StopCharSessionResponse> stopChargingSession(
            @Valid @RequestBody StopCharSessionRequest request
    ) {
        StopCharSessionResponse response = staffCharSessionService.stopChargingSession(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session detail", description = "Get detailed information of a charging session")
    public ResponseEntity<ViewCharSessionResponse> getSessionById(
            @Parameter(description = "Session ID") @PathVariable Long sessionId
    ) {
        ViewCharSessionResponse response = staffCharSessionService.getCharSessionById(sessionId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/active")
    @Operation(summary = "Get active sessions", description = "Get list of currently active charging sessions at a station")
    public ResponseEntity<List<ViewCharSessionResponse>> getActiveSessionsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId
    ) {
        List<ViewCharSessionResponse> sessions = staffCharSessionService.getActiveCharSessionsByStation(stationId);
        return ResponseEntity.ok(sessions);
    }
}
