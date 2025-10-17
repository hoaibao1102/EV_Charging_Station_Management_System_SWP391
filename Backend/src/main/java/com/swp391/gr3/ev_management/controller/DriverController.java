package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@Tag(name = "Drivers", description = "APIs for driver management")
@RequiredArgsConstructor
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private TokenService tokenService;

    // ✅ Driver xem hồ sơ chính mình (qua token)
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/own-profile-driver")
    @Operation(summary = "Get own driver profile", description = "Driver retrieves their own profile information")
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse driver = driverService.getByUserId(userId);
        return ResponseEntity.ok(driver);
    }

    // ✅ Driver cập nhật hồ sơ
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/updateProfile")
    @Operation(summary = "Update own driver profile", description = "Driver updates their own profile information")
    public ResponseEntity<DriverResponse> updateOwnProfile(
            HttpServletRequest request,
            @Valid @RequestBody DriverUpdateRequest updateRequest) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse updated = driverService.updateDriverProfile(userId, updateRequest);
        return ResponseEntity.ok(updated);
    }

}
