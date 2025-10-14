package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.emuns.DriverStatus;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.TokenService;
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
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long driverId = tokenService.extractUserIdFromRequest(request);
        DriverResponse driver = driverService.getByDriverId(driverId);
        return ResponseEntity.ok(driver);
    }

    // ✅ Driver cập nhật hồ sơ
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/profile")
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
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getByUserId(driverId));
    }

    // ✅ Admin xem tất cả driver
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // ✅ Admin cập nhật trạng thái driver (ACTIVE, SUSPENDED,...)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/status")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long userId,
            @RequestParam("status") DriverStatus status) {
        return ResponseEntity.ok(driverService.updateStatus(userId, status));
    }

    // ✅ Admin: Tạo driver cho 1 user (nâng cấp quyền)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}")
    public ResponseEntity<DriverResponse> createDriverForUser(
            @PathVariable Long userId,
            @RequestBody(required = false) DriverRequest request) {
        DriverRequest effective = request != null ? request : new DriverRequest();
        DriverResponse created = driverService.createDriverProfile(userId, effective);
        return ResponseEntity.ok(created);
    }

    // ✅ Admin: Lọc driver theo trạng thái
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-status")
    public ResponseEntity<List<DriverResponse>> getDriversByStatus(@RequestParam DriverStatus status) {
        return ResponseEntity.ok(driverService.getDriversByStatus(status));
    }

    // ✅ Admin: Tìm kiếm theo tên (contains, ignore case)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-name")
    public ResponseEntity<List<DriverResponse>> getDriversByName(@RequestParam String name) {
        return ResponseEntity.ok(driverService.getDriversByName(name));
    }

    // ✅ Admin: Tìm kiếm theo số điện thoại (contains)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-phone")
    public ResponseEntity<List<DriverResponse>> getDriversByPhone(@RequestParam("phoneNumber") String phoneNumber) {
        return ResponseEntity.ok(driverService.getDriversByPhoneNumber(phoneNumber));
    }

}
