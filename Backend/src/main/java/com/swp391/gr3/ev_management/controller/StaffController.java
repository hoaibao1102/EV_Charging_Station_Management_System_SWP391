package com.swp391.gr3.ev_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.gr3.ev_management.dto.request.StopSessionForStaffRequest;
import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.dto.response.StaffResponse;
import com.swp391.gr3.ev_management.dto.response.StationStaffResponse;
import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.service.ChargingSessionService;
import com.swp391.gr3.ev_management.service.StaffService;
import com.swp391.gr3.ev_management.service.StaffStationService;
import com.swp391.gr3.ev_management.service.TokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController // ✅ Đánh dấu đây là REST Controller — trả dữ liệu JSON
@RequestMapping("/api/staff") // ✅ Tất cả endpoint trong controller này bắt đầu bằng /api/staff
@Tag(name = "Staff", description = "APIs for Staff operations")
// ✅ Swagger tag — gom nhóm các API dành cho STAFF
@RequiredArgsConstructor // ✅ Lombok tự động tạo constructor cho các field final (Dependency Injection)
public class StaffController {

    private final StaffStationService staffStationService; // ✅ Service quản lý mối quan hệ Staff - Station
    private final StaffService staffService; // ✅ Service xử lý logic liên quan đến hồ sơ và tài khoản staff
    private final TokenService tokenService; // ✅ Dùng để trích xuất userId từ JWT token
    private final ChargingSessionService chargingSessionService;

    // =========================================================================
    // ✅ 1. STAFF: CẬP NHẬT THÔNG TIN CÁ NHÂN (PROFILE)
    // =========================================================================
    @PreAuthorize("hasRole('STAFF')") // 🔒 Chỉ nhân viên (STAFF) mới được quyền gọi API này
    @PutMapping("/profile") // 🔗 Endpoint: PUT /api/staff/profile
    public ResponseEntity<StaffResponse> updateProfile(
            HttpServletRequest request, // ✅ Dùng để lấy token của người đang đăng nhập
            @RequestBody UpdateStaffProfileRequest profileRequest // ✅ Body chứa thông tin mới (tên, email, phone, v.v.)
    ) {
        // 🟢 Lấy ID của user (staff hiện tại) từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🟢 Gọi service để cập nhật hồ sơ của staff dựa theo userId và thông tin trong request
        StaffResponse updated = staffService.updateProfile(userId, profileRequest);

        // 🟢 Trả về HTTP 200 cùng dữ liệu profile đã cập nhật
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // ✅ 2. STAFF: CẬP NHẬT MẬT KHẨU
    // =========================================================================
    @PreAuthorize("hasRole('STAFF')") // 🔒 Chỉ nhân viên được phép đổi mật khẩu của mình
    @PutMapping("/password") // 🔗 Endpoint: PUT /api/staff/password
    public ResponseEntity<String> updatePassword(
            HttpServletRequest request, // ✅ Lấy request để truy xuất user từ token
            @RequestBody UpdatePasswordRequest passwordRequest // ✅ Body chứa mật khẩu cũ, mới và xác nhận mật khẩu
    ) {
        // 🟢 Lấy userId của nhân viên hiện tại từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🟢 Gọi service để cập nhật mật khẩu cho nhân viên
        staffService.updatePassword(userId, passwordRequest);

        // 🟢 Trả về HTTP 200 cùng thông báo thành công
        return ResponseEntity.ok("Password updated successfully");
    }

    // =========================================================================
    // ✅ 3. STAFF: XEM THÔNG TIN HỒ SƠ CỦA CHÍNH MÌNH
    // =========================================================================
    @PreAuthorize("hasRole('STAFF')") // 🔒 Chỉ nhân viên được quyền xem hồ sơ của chính mình
    @GetMapping("/own-profile-staff") // 🔗 Endpoint: GET /api/staff/own-profile-staff
    @Operation(
            summary = "Get own staff profile",
            description = "Staff retrieves their own profile information" // 📝 Swagger mô tả API
    )
    public ResponseEntity<StationStaffResponse> getOwnProfile(
            HttpServletRequest request // ✅ Dùng để lấy token trong header
    ) {
        // 🟢 Trích xuất ID của user (staff) từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🟢 Gọi service để lấy thông tin chi tiết của staff (bao gồm cả thông tin station mà họ thuộc về)
        StationStaffResponse staff = staffStationService.getStaffByUserId(userId);

        // 🟢 Trả về HTTP 200 cùng dữ liệu hồ sơ staff
        return ResponseEntity.ok(staff);
    }

    // =========================================================================
    // ✅ 4. STAFF: DỪNG PHIÊN SẠC
    // =========================================================================

    @PostMapping("/staff-stop-session") // 🔗 POST /api/charging-sessions/driver-stop
    @Operation(summary = "Staff stops the charging session", description = "Staff stops the charging session using session ID and user ID")
    public ResponseEntity<StopCharSessionResponse> staffStopSession(
            @RequestBody StopSessionForStaffRequest body // ✅ Chứa sessionId và finalSoc
    ) {
        // 🟢 Gọi service để dừng phiên sạc, truyền finalSoc nếu có
        StopCharSessionResponse res =
                chargingSessionService.staffStopSession(body.getSessionId(), body.getFinalSoc());

        // 🟢 Trả về 200 OK + thông tin sau khi dừng
        return ResponseEntity.ok(res);
    }
}