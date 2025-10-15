package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.emuns.DriverStatus;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/profile")
    @Operation(summary = "Get own driver profile", description = "Driver retrieves their own profile information")
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long driverId = tokenService.extractUserIdFromRequest(request);
        DriverResponse driver = driverService.getByDriverId(driverId);
        return ResponseEntity.ok(driver);
    }

    // ✅ Driver cập nhật hồ sơ
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/profile")
    @Operation(summary = "Update own driver profile", description = "Driver updates their own profile information")
    public ResponseEntity<DriverResponse> updateOwnProfile(
            HttpServletRequest request,
            @Valid @RequestBody DriverUpdateRequest updateRequest) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse updated = driverService.updateDriverProfile(userId, updateRequest);
        return ResponseEntity.ok(updated);
    }

    // ✅ Admin xem chi tiết driver

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{driverId}")
    @Operation(summary = "Get driver by ID", description = "Admin retrieves driver information by driver ID")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getByUserId(driverId));
    }

    // ✅ Admin xem tất cả driver
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all drivers", description = "Admin retrieves a list of all drivers")
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // ✅ Admin cập nhật trạng thái driver (ACTIVE, SUSPENDED,...)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/status")
   @Operation(summary = "Update driver status", description = "Admin updates the status of a driver")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long userId,
            @RequestParam("status") DriverStatus status) {
        return ResponseEntity.ok(driverService.updateStatus(userId, status));
    }


    // ✅ Admin: Lọc driver theo trạng thái
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-status")
    @Operation(summary = "Get drivers by status", description = "Admin retrieves drivers filtered by their status")
    public ResponseEntity<List<DriverResponse>> getDriversByStatus(@RequestParam DriverStatus status) {
        return ResponseEntity.ok(driverService.getDriversByStatus(status));
    }

    // ✅ Admin: Tìm kiếm theo tên (contains, ignore case)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-name")
    @Operation(summary = "Get drivers by name", description = "Admin retrieves drivers filtered by name (contains, ignore case)")
    public ResponseEntity<List<DriverResponse>> getDriversByName(@RequestParam String name) {
        return ResponseEntity.ok(driverService.getDriversByName(name));
    }

    // ✅ Admin: Tìm kiếm theo số điện thoại (contains)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-phone")
    @Operation(summary = "Get drivers by phone number", description = "Admin retrieves drivers filtered by phone number (contains)")
    public ResponseEntity<List<DriverResponse>> getDriversByPhone(@RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(driverService.getDriversByPhoneNumber(phoneNumber));
    }

}
