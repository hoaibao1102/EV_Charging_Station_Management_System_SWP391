package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.CreateStationStaffRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdateAdminProfileRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.DTO.response.*;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.service.*;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "APIs for admin operations")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final TokenService tokenService;
    private final DriverService driverService;
    private final StaffService staffService;
    private final StaffStationService staffStationService;
    private final ChargingSessionService chargingSessionService;
    private final NotificationsService notificationsService;
    private final AdminService adminService;

    // ----------------------ADMIN: Quản lý USERS----------------------------- //


    // ADMIN: xem tất cả user
    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Admin get list of users")
    public List<GetUsersResponse> getAllUsers() {
        return userService.findAll() // đảm bảo method này gọi repo có fetch như trên
                .stream()
                .map(user -> new GetUsersResponse(
                        user.getUserId(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getName(),
                        user.getDateOfBirth(),
                        user.getGender(),
                        user.getAddress(),
                        extractStatus(user),                 // 👈 status
                        user.getRole() != null ? user.getRole().getRoleName() : null
                ))
                .toList();
    }

    private String extractStatus(User user) {
        // Ưu tiên theo role, có thể đổi thứ tự nếu bạn muốn
        if (user.getDriver() != null && user.getDriver().getStatus() != null) {
            return user.getDriver().getStatus().name();   // DriverStatus enum
        }
        if (user.getStaffs() != null && user.getStaffs().getStatus() != null) {
            return user.getStaffs().getStatus().name();   // StaffStatus enum
        }
        if (user.getAdmin() != null) {
            // Nếu Admin cũng có status thì lấy tương tự:
            // return user.getAdmin().getStatus().name();
            return "ACTIVE"; // hoặc null / giá trị mặc định nếu Admin không có status
        }
        return null;
    }

    // ----------------------ADMIN: Quản lý STAFF----------------------------- //

    // ADMIN: đăng ký user -> gắn làm staff
    @PostMapping(value = "/register-staff", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register staff", description = "Admin register staff for a user")
    public ResponseEntity<?> adminRegisterStaff(@Valid @RequestBody CreateStationStaffRequest req) {
        Map<String, Object> response = userService.registerStaffAndAssignStation(req.getUser(), req.getStationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/status-staffs")
    @Operation(summary = "Update staffs status", description = "Admin updates the status of a staff")
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable Long userId,
            @RequestParam("status") StaffStatus status) {
        return ResponseEntity.ok(staffService.updateStatus(userId, status));
    }



    // ----------------------ADMIN: Quản lý DRIVER----------------------------- //

    // ✅ Admin xem tất cả driver
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-divers")
    @Operation(summary = "Get all drivers", description = "Admin retrieves a list of all drivers")
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // ✅ Admin cập nhật trạng thái driver (ACTIVE, SUSPENDED,...)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/status-divers")
    @Operation(summary = "Update driver status", description = "Admin updates the status of a driver")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long userId,
            @RequestParam("status") DriverStatus status) {
        return ResponseEntity.ok(driverService.updateStatus(userId, status));
    }

    // ----------------------ADMIN: Quản lý PROFILE----------------------------- //

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateAdminProfileRequest updateRequest) {
        try {
            Long userId = tokenService.extractUserIdFromRequest(request);
            adminService.updateProfile(userId, updateRequest);
            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công"));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            HttpServletRequest request,
            @RequestBody UpdatePasswordRequest updateRequest) {
        try {
            Long userId = tokenService.extractUserIdFromRequest(request);
            adminService.updatePassword(userId, updateRequest);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }



    // ----------------------ADMIN: Quản lý NOTIFICATIONS----------------------------- //

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-notifications")
    @Operation(summary = "Get all notifications", description = "Get all notifications for the logged-in user")
    public ResponseEntity<?> getAllNotifications(org.springframework.security.core.Authentication auth) {
        Long userId = Long.valueOf(auth.getName()); // vì principal = userId string
        var notifications = notificationsService.getNotificationsByUser(userId);

        if (notifications == null || notifications.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có thông báo"));
        }
        return ResponseEntity.ok(notifications);
    }
}
