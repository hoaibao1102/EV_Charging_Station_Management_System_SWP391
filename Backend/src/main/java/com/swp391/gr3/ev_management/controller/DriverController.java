package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.DriverRequest;
import com.swp391.gr3.ev_management.dto.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.entity.DriverStatus;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private TokenService tokenService;

    // ✅ Driver xem hồ sơ chính mình (qua token)
    @GetMapping("/profile")
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse driver = driverService.getById(userId);
        return ResponseEntity.ok(driver);
    }

    // ✅ Driver cập nhật hồ sơ
    @PutMapping("/profile")
    public ResponseEntity<DriverResponse> updateOwnProfile(
            HttpServletRequest request,
            @Valid @RequestBody DriverUpdateRequest updateRequest) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse updated = driverService.updateDriverProfile(userId, updateRequest);
        return ResponseEntity.ok(updated);
    }

    // ✅ Admin xem chi tiết driver
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getById(driverId));
    }

    // ✅ Admin xem tất cả driver
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // ✅ Admin cập nhật trạng thái driver (ACTIVE, SUSPENDED,...)
    @PutMapping("/{userId}/status")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long userId,
            @RequestParam("status") DriverStatus status) {
        return ResponseEntity.ok(driverService.updateStatus(userId, status));
    }


}
