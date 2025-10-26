package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.*;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.service.ChargingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/charging-sessions")
@RequiredArgsConstructor
@Tag(name = "Staff Charging Session", description = "APIs for staff to manage charging sessions")
public class ChargingSessionController {

    private final ChargingSessionService chargingSessionService;

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/start")
    @Operation(summary = "Start charging session", description = "Staff starts a new charging session for a confirmed booking")
    public ResponseEntity<StartCharSessionResponse> startChargingSession(
            @Valid @RequestBody StartCharSessionRequest request
    ) {
        StartCharSessionResponse response = chargingSessionService.startChargingSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop charging session", description = "Staff stops an ongoing charging session and records final energy")
    public ResponseEntity<StopCharSessionResponse> stopChargingSession(
            @Valid @RequestBody StopCharSessionRequest request
    ) {
        StopCharSessionResponse response = chargingSessionService.stopChargingSession(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("all-session")
    public List<ChargingSessionResponse> getAllSession() {
        return chargingSessionService.getAll()
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
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session detail", description = "Get detailed information of a charging session")
    public ResponseEntity<ViewCharSessionResponse> getSessionById(
            @Parameter(description = "Session ID") @PathVariable Long sessionId
    ) {
        ViewCharSessionResponse response = chargingSessionService.getCharSessionById(sessionId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/active")
    @Operation(summary = "Get active sessions", description = "Get list of currently active charging sessions at a station")
    public ResponseEntity<List<ViewCharSessionResponse>> getActiveSessionsByStation(
            @Parameter(description = "Station ID") @RequestParam Long stationId
    ) {
        List<ViewCharSessionResponse> sessions = chargingSessionService.getActiveCharSessionsByStation(stationId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/session/{sessionId}/status")
    public ChargingStatusResponse getChargingStatus(
            @PathVariable Long sessionId,
            @RequestParam int initialSoc,
            @RequestParam String connectorType // VD: "DC" hoặc "AC"
    ) {
        ChargingSession session = chargingSessionService.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        LocalDateTime start = session.getStartTime();
        long minutes = Duration.between(start, LocalDateTime.now()).toMinutes();

        double ratePerHour = connectorType.equalsIgnoreCase("DC") ? 25.0 : 10.0;
        double ratePerMinute = ratePerHour / 60.0;

        double currentSoc = Math.min(100, initialSoc + minutes * ratePerMinute);
        double energyKWh = (currentSoc - initialSoc) * 0.5; // tuỳ bạn muốn mô phỏng
        return ChargingStatusResponse.builder()
                .sessionId(sessionId)
                .currentSoc(currentSoc)
                .energyKWh(energyKWh)
                .minutesElapsed(minutes)
                .build();
    }
}
