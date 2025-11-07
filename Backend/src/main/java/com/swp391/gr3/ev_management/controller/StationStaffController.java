package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.StationStaffResponse;
import com.swp391.gr3.ev_management.service.StaffStationService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu class này là REST Controller (tự động trả JSON)
@RequestMapping("/api/station-staff") // ✅ Tất cả endpoint trong controller này bắt đầu bằng /api/station-staff
@Tag(name = "Station-staff", description = "APIs for station-staff operations")
// ✅ Dùng cho Swagger UI — nhóm API này thuộc phần quản lý mối quan hệ giữa Staff và Station
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho các field final (Dependency Injection)
public class StationStaffController {

    private final StaffStationService staffStationService; // ✅ Service quản lý mối quan hệ Staff - Station
    private final TokenService tokenService;               // ✅ Service dùng để trích xuất userId từ JWT token

    // =========================================================================
    // ✅ 1. ADMIN: CẬP NHẬT (THAY ĐỔI) TRẠM ĐƯỢC PHÂN CÔNG CHO NHÂN VIÊN
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền thay đổi nhân viên thuộc trạm nào
    @PutMapping("/{staffId}/station") // 🔗 Endpoint: PUT /api/station-staff/{staffId}/station?stationId=123
    public ResponseEntity<StationStaffResponse> updateStationForStaff(
            @PathVariable Long staffId,  // ✅ ID của nhân viên cần thay đổi trạm
            @RequestParam Long stationId  // ✅ ID của trạm mới được gán cho nhân viên
    ) {
        // 🟢 Gọi service để cập nhật thông tin nhân viên (gán staff vào trạm stationId)
        StationStaffResponse response = staffStationService.updateStation(staffId, stationId);

        // 🟢 Trả về HTTP 200 cùng dữ liệu phản hồi (bao gồm thông tin staff + station mới)
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 2. ADMIN: LẤY DANH SÁCH TOÀN BỘ NHÂN VIÊN VÀ TRẠM ĐƯỢC PHÂN CÔNG
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền xem danh sách toàn bộ nhân viên và trạm của họ
    @GetMapping // 🔗 Endpoint: GET /api/station-staff
    @Operation(
            summary = "Get all staff-station assignments",
            description = "Admin gets all staff with their assigned charging stations"
    )
    public ResponseEntity<List<StationStaffResponse>> getAll() {
        // 🟢 Gọi service để lấy danh sách tất cả các nhân viên và trạm mà họ được gán vào
        List<StationStaffResponse> list = staffStationService.getAll();

        // 🟢 Trả về HTTP 200 cùng danh sách kết quả
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // ✅ 3. STAFF: XEM TRẠM MÀ MÌNH ĐƯỢC PHÂN CÔNG
    // =========================================================================
    @GetMapping("/me") // 🔗 Endpoint: GET /api/station-staff/me
    @Operation(
            summary = "Get my assigned station",
            description = "Staff gets their assigned charging station" // 📝 Swagger mô tả
    )
    public ResponseEntity<List<StationStaffResponse>> getMyStation(
            HttpServletRequest request // ✅ Dùng để lấy JWT token từ header
    ) {
        // 🟢 Trích xuất userId (nhân viên hiện tại) từ token trong request
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🟢 Gọi service để lấy danh sách trạm mà nhân viên này được phân công (thường chỉ 1 trạm)
        List<StationStaffResponse> response = staffStationService.getByStationStaffUserId(userId);

        // 🟢 Trả về HTTP 200 cùng dữ liệu trạm tương ứng
        return ResponseEntity.ok(response);
    }
}
