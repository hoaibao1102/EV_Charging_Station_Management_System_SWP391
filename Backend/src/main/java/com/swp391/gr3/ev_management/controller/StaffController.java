package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.service.StaffStationService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "APIs for Staff operations")
@RequiredArgsConstructor
public class StaffController {

    @Autowired
    private StaffStationService staffStationService;

    @Autowired
    private TokenService tokenService;

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/own-profile-staff")
    @Operation(summary = "Get own staff profile", description = "Staff retrieves their own profile information")
    public ResponseEntity<StationStaffResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        StationStaffResponse staff = staffStationService.getStaffByUserId(userId);
        return ResponseEntity.ok(staff);
    }
}
