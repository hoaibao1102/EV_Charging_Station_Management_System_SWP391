package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.service.ChargingSessionService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TokenService tokenService;

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/start")
    @Operation(summary = "Start charging session", description = "Staff starts a new charging session for a confirmed booking")
    public ResponseEntity<StartCharSessionResponse> startChargingSession(
            @Valid @RequestBody StartCharSessionRequest request
    ) {
        StartCharSessionResponse response = chargingSessionService.startChargingSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("all-session")
    @Operation(summary = "Get all sessions", description = "Get list of all charging sessions")
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

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/session/{sessionId}/status")
    @Operation(summary = "Get charging status", description = "Get current charging status (SoC, energy delivered) of an ongoing session")
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

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/stations/{stationId}/charging-sessions")
    @Operation(summary = "Get all sessions by station", description = "Get all charging sessions for a specific station")
    public ResponseEntity<List<ViewCharSessionResponse>> getAllByStation(@PathVariable Long stationId) {
        List<ViewCharSessionResponse> res = chargingSessionService.getAllSessionsByStation(stationId);
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver-stop")
    @Operation(summary = "Driver stops their own charging session", description = "Driver stops their own charging session using session ID")
    public ResponseEntity<StopCharSessionResponse> driverStopSession(
            @RequestBody StopCharSessionRequest body,
            HttpServletRequest httpReq
    ) {
        Long userId = tokenService.extractUserIdFromRequest(httpReq);   // <- lấy từ token
        StopCharSessionResponse res =
                chargingSessionService.driverStopSession(body.getSessionId(), userId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/charging-sessions/current")
    @Operation(summary = "Get current active session for driver",
            description = "Driver retrieves their currently active charging session")
    public ResponseEntity<ViewCharSessionResponse> getCurrentSession(HttpServletRequest httpReq) {
        Long userId = tokenService.extractUserIdFromRequest(httpReq);

        List<ChargingSession> all = chargingSessionService.getAll(); // hoặc tạo repo method riêng để tối ưu
        ChargingSession current = all.stream()
                .filter(s -> s.getStatus() == ChargingSessionStatus.IN_PROGRESS)
                .filter(s -> s.getBooking() != null
                        && s.getBooking().getVehicle() != null
                        && s.getBooking().getVehicle().getDriver() != null
                        && s.getBooking().getVehicle().getDriver().getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ErrorException("Bạn không có phiên sạc nào đang hoạt động."));

        ViewCharSessionResponse res = chargingSessionService.getCharSessionById(current.getSessionId());
        return ResponseEntity.ok(res);
    }
}
