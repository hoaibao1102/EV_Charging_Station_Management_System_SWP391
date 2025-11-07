package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.CreateStationStaffRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateAdminProfileRequest;
import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.dto.response.*;
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

@RestController // Đánh dấu đây là REST controller (trả về JSON/XML thay vì trả về view)
@RequestMapping("/api/admin") // Prefix chung cho tất cả endpoint của AdminController
@Tag(name = "Admin", description = "APIs for admin operations") // Dùng cho tài liệu Swagger: nhóm API "Admin"
@RequiredArgsConstructor // Lombok: sinh constructor với tất cả field final (DI qua constructor)
public class AdminController {
    // ====== Các service được inject qua constructor (do @RequiredArgsConstructor) ======
    private final UserService userService;                 // Xử lý nghiệp vụ liên quan đến User
    private final TokenService tokenService;               // Xử lý token (rút userId từ request)
    private final DriverService driverService;             // Nghiệp vụ tài xế
    private final StaffService staffService;               // Nghiệp vụ nhân viên trạm
    private final NotificationsService notificationsService; // Nghiệp vụ thông báo
    private final AdminService adminService;               // Nghiệp vụ riêng cho admin (profile/password, ...)

    // ----------------------ADMIN: Quản lý USERS----------------------------- //

    // ADMIN: xem tất cả user
    @GetMapping("/all-users") // HTTP GET: /api/admin/all-users
    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép user có ROLE_ADMIN truy cập
    @Operation(summary = "Get all users", description = "Admin get list of users with sessions") // Mô tả Swagger
    public List<GetUsersResponse> getAllUsers() {
        // Gọi service để lấy toàn bộ user kèm thông tin session
        // Trả thẳng List<GetUsersResponse> (Spring sẽ tự convert sang JSON)
        return userService.getAllUsersWithSessions();
    }

    // ----------------------ADMIN: Quản lý STAFF----------------------------- //

    // ADMIN: đăng ký user -> gắn làm staff
    @PostMapping(value = "/register-staff", consumes = "application/json", produces = "application/json") // HTTP POST JSON
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @Operation(summary = "Register staff", description = "Admin register staff for a user")
    public ResponseEntity<?> adminRegisterStaff(@Valid @RequestBody CreateStationStaffRequest req) {
        // @Valid: kích hoạt validate cho DTO CreateStationStaffRequest (theo annotation trong DTO)
        // @RequestBody: đọc JSON request body và map vào req
        // Nghiệp vụ: đăng ký một user thành staff và gán vào stationId tương ứng
        Map<String, Object> response = userService.registerStaffAndAssignStation(req.getUser(), req.getStationId());
        // Trả về HTTP 201 CREATED cùng dữ liệu response (có thể chứa thông tin staff/station)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @PutMapping("/{userId}/status-staffs") // HTTP PUT: /api/admin/{userId}/status-staffs?status=ACTIVE|INACTIVE|...
    @Operation(summary = "Update staffs status", description = "Admin updates the status of a staff")
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable Long userId,           // Lấy userId từ path
            @RequestParam("status") StaffStatus status) { // Lấy status (enum) từ query param
        // Gọi service cập nhật trạng thái staff theo userId
        return ResponseEntity.ok(staffService.updateStatus(userId, status));
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @GetMapping("/all-staffs") // HTTP GET: /api/admin/all-staffs
    @Operation(summary = "Get all staffs", description = "Admin retrieves a list of all staffs")
    public ResponseEntity<List<StaffResponse>> getAllStaffs() {
        // Lấy danh sách tất cả staff
        return ResponseEntity.ok(staffService.getAll());
    }


    // ----------------------ADMIN: Quản lý DRIVER----------------------------- //

    // ✅ Admin xem tất cả driver
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @GetMapping("/all-divers") // LƯU Ý: endpoint đang viết "divers" (có thể là typo của "drivers"), giữ nguyên theo yêu cầu
    @Operation(summary = "Get all drivers", description = "Admin retrieves a list of all drivers")
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        // Lấy danh sách tất cả tài xế
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // ✅ Admin cập nhật trạng thái driver (ACTIVE, SUSPENDED,...)
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @PutMapping("/{userId}/status-divers") // LƯU Ý: endpoint "divers" (giữ nguyên), PUT: /api/admin/{userId}/status-divers?status=ACTIVE|...
    @Operation(summary = "Update driver status", description = "Admin updates the status of a driver")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long userId,                 // userId tài xế (theo user)
            @RequestParam("status") DriverStatus status) { // Trạng thái mới (enum)
        // Cập nhật trạng thái driver và trả về thông tin DriverResponse
        return ResponseEntity.ok(driverService.updateStatus(userId, status));
    }

    // ----------------------ADMIN: Quản lý PROFILE----------------------------- //

    @PutMapping("/profile") // HTTP PUT: /api/admin/profile
    public ResponseEntity<Map<String, String>> updateProfile(
            HttpServletRequest request,                   // Dùng để lấy token từ header/cookie
            @RequestBody UpdateAdminProfileRequest updateRequest) { // Dữ liệu profile mới của admin
        try {
            // Trích xuất userId của admin từ token trong request (thường đọc từ Authorization header)
            Long userId = tokenService.extractUserIdFromRequest(request);
            // Gọi service để cập nhật profile theo userId
            adminService.updateProfile(userId, updateRequest);
            // Trả về message thành công
            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công"));
        } catch (Exception e) {
            // Bắt mọi lỗi và trả về 400 cùng thông điệp thất bại
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/password") // HTTP PUT: /api/admin/password
    public ResponseEntity<Map<String, String>> updatePassword(
            HttpServletRequest request,                    // Lấy token để xác định admin hiện tại
            @RequestBody UpdatePasswordRequest updateRequest) { // Chứa oldPassword/newPassword (tuỳ DTO)
        try {
            // Lấy userId từ token
            Long userId = tokenService.extractUserIdFromRequest(request);
            // Gọi service để cập nhật mật khẩu, có thể kiểm tra mật khẩu cũ, tính hợp lệ, mã hoá...
            adminService.updatePassword(userId, updateRequest);
            // Trả về message thành công
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            // Trường hợp lỗi dữ liệu đầu vào (ví dụ: mật khẩu cũ không đúng, mật khẩu mới không hợp lệ...)
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Lỗi không lường trước được -> trả về 500 kèm thông điệp
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }



    // ----------------------ADMIN: Quản lý NOTIFICATIONS----------------------------- //

    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
    @GetMapping("/all-notifications") // HTTP GET: /api/admin/all-notifications
    @Operation(summary = "Get all notifications", description = "Get all notifications for the logged-in user")
    public ResponseEntity<?> getAllNotifications(org.springframework.security.core.Authentication auth) {
        // auth.getName() đang được giả định là userId ở dạng String (vì principal = userId string)
        Long userId = Long.valueOf(auth.getName()); // Chuyển về Long

        // Lấy danh sách thông báo của userId hiện tại
        var notifications = notificationsService.getNotificationsByUser(userId);

        if (notifications == null || notifications.isEmpty()) {
            // Nếu không có thông báo, trả về 200 cùng một message thân thiện
            return ResponseEntity.ok(Map.of("message", "Không có thông báo"));
        }
        // Nếu có, trả danh sách thông báo (Spring tự convert sang JSON)
        return ResponseEntity.ok(notifications);
    }
}
