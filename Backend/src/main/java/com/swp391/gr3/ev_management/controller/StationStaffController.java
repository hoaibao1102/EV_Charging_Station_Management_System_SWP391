package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.service.StaffStationService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/station-staff")
@Tag(name = "Station-staff", description = "APIs for station-staff operations")
@RequiredArgsConstructor
public class StationStaffController {

    private final StaffStationService staffStationService;

    private final TokenService tokenService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{staffId}/station")
    public ResponseEntity<StationStaffResponse> updateStationForStaff(
            @PathVariable Long staffId,
            @RequestParam Long stationId
    ) {
        return ResponseEntity.ok(staffStationService.updateStation(staffId, stationId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all staff-station assignments", description = "Admin gets all staff with their assigned charging stations")
    public ResponseEntity<List<StationStaffResponse>> getAll() {
        List<StationStaffResponse> list = staffStationService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my assigned station", description = "Staff gets their assigned charging station")
    public ResponseEntity<List<StationStaffResponse>> getMyStation(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        List<StationStaffResponse> response = staffStationService.getByStationStaffUserId(userId);
        return ResponseEntity.ok(response);
    }
}
