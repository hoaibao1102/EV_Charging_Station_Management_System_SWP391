package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverChangePasswordRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.VehicleResponse;
import com.swp391.gr3.ev_management.service.DriverService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@Tag(name = "Drivers", description = "APIs for driver management")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    private final TokenService tokenService;

    @PreAuthorize("hasRole('DRIVER')")
    @DeleteMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Remove vehicle", description = "Driver removes a vehicle from their profile")
    public ResponseEntity<Void> removeVehicle(
            HttpServletRequest request,
            @PathVariable Long vehicleId) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        driverService.removeVehicle(userId, vehicleId);
        return ResponseEntity.noContent().build();
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

    // ✅ Driver cập nhật mật khẩu
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/password")
    @Operation(summary = "Update own driver Password", description = "Driver updates their own Password")
    public ResponseEntity<DriverResponse> updateOwnPassword(HttpServletRequest request,@Valid @RequestBody DriverChangePasswordRequest req) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse updated = driverService.updateDriverPassword(userId, req.getOldPassword(), req.getNewPassword(), req.getConfirmNewPassword());
        return ResponseEntity.ok(updated);
    }
    
    /**
     * UC-04: Driver thêm xe vào hồ sơ
     * BR-02: Xe phải thuộc về driver đang đăng nhập
     * BR-03: Kiểm tra model tồn tại và license plate chưa được đăng ký
     */
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/vehicles")
    @Operation(summary = "Add vehicle", description = "Driver adds a new vehicle to their profile")
    public ResponseEntity<VehicleResponse> addVehicle(
            HttpServletRequest request,
            @Valid @RequestBody AddVehicleRequest addRequest) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        VehicleResponse vehicle = driverService.addVehicle(userId, addRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

   
    // ✅ Driver xem hồ sơ chính mình (qua token)
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/profile")
    @Operation(summary = "Get own driver profile", description = "Driver retrieves their own profile information")
    public ResponseEntity<DriverResponse> getOwnProfile(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        DriverResponse driver = driverService.getByUserId(userId);
        return ResponseEntity.ok(driver);
    }
    
    /**
     * UC-04: Xem danh sách xe của driver
     */
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/vehicles")
    @Operation(summary = "Get my vehicles", description = "Driver retrieves list of their vehicles")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(HttpServletRequest request) {
        Long userId = tokenService.extractUserIdFromRequest(request);
        List<VehicleResponse> vehicles = driverService.getMyVehicles(userId);
        return ResponseEntity.ok(vehicles);
    }
}
