package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.ViewCharSessionRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/sessions")
@RequiredArgsConstructor
@Tag(name = "Staff Charging Session", description = "APIs for staff to manage charging sessions")
public class ChargingSessionController {
    private final StaffCharSessionService chargingService;

    @PostMapping("/start")
    @Operation(summary = "Start charging session", description = "Staff starts a new charging session for a confirmed booking")
    public ResponseEntity<StartCharSessionResponse> startChargingSession(
            @Valid @RequestBody StartCharSessionRequest request
    ) {
        StartCharSessionResponse response = chargingService.startChargingSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop charging session", description = "Staff stops an ongoing charging session and records final energy")
    public ResponseEntity<StopCharSessionResponse> stopChargingSession(
            @Valid @RequestBody StopCharSessionRequest request
    ) {
        StopCharSessionResponse response = chargingService.stopChargingSession(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session detail", description = "Get detailed information of a charging session")
    public ResponseEntity<ViewCharSessionResponse> getSessionById(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        ViewCharSessionResponse response = chargingService.getCharSessionById(sessionId, staffId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active sessions", description = "Get list of currently active charging sessions at a station")
    public ResponseEntity<List<ViewCharSessionResponse>> getActiveSessionsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<ViewCharSessionResponse> sessions = chargingService.getActiveCharSessionsByStation(stationId, staffId);
        return ResponseEntity.ok(sessions);
    }
}
