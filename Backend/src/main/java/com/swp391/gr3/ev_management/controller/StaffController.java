package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import com.swp391.gr3.ev_management.service.StaffService;
import com.swp391.gr3.ev_management.service.StaffStationService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "APIs for Staff operations")
@RequiredArgsConstructor
public class StaffController {

    private final StaffStationService staffStationService;

    private final StaffService staffService;

    private final TokenService tokenService;

    @PreAuthorize("hasRole('STAFF')")
    @PutMapping("/profile")
    public ResponseEntity<StaffResponse> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateStaffProfileRequest profileRequest
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        return ResponseEntity.ok(staffService.updateProfile(userId, profileRequest));
    }

    @PreAuthorize("hasRole('STAFF')")
    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(
            HttpServletRequest request,
            @RequestBody UpdatePasswordRequest passwordRequest
    ) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        staffService.updatePassword(userId, passwordRequest);
        return ResponseEntity.ok("Password updated successfully");
    }

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/own-profile-staff")
    @Operation(summary = "Get own staff profile", description = "Staff retrieves their own profile information")
    public ResponseEntity<StationStaffResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        StationStaffResponse staff = staffStationService.getStaffByUserId(userId);
        return ResponseEntity.ok(staff);
    }


}
