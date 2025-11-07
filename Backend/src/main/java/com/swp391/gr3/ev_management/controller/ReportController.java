package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.CreateReportRequest;
import com.swp391.gr3.ev_management.dto.response.ReportResponse;
import com.swp391.gr3.ev_management.service.ReportService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu đây là REST controller (tự động trả JSON thay vì view)
@RequiredArgsConstructor // ✅ Lombok: tự động sinh constructor cho các field final (Dependency Injection)
@RequestMapping(value = "/api/incidents", produces = MediaType.APPLICATION_JSON_VALUE)
// ✅ Tất cả endpoint trong controller này sẽ bắt đầu bằng /api/incidents và trả về JSON
@Tag(name = "Staff Incident", description = "APIs for staff to manage incident reports")
// ✅ Dùng cho Swagger UI để nhóm các API này vào mục “Staff Incident”
public class ReportController {

    private final ReportService reportService; // ✅ Service xử lý nghiệp vụ liên quan đến báo cáo sự cố (incident)
    private final TokenService tokenService;   // ✅ Service dùng để trích xuất userId từ token trong request

    // =========================================================================
    // ✅ 1. STAFF: TẠO MỚI BÁO CÁO SỰ CỐ
    // =========================================================================
    @PreAuthorize("hasRole('STAFF')") // 🔒 Chỉ nhân viên trạm (STAFF) mới có quyền tạo báo cáo
    @PostMapping("/create") // 🔗 Endpoint: POST /api/incidents/create
    @Operation(
            summary = "Create a new incident", // 📝 Mô tả ngắn gọn cho Swagger
            description = "Create a new incident report by a station staff" // 📝 Mô tả chi tiết
    )
    public ResponseEntity<ReportResponse> createIncident(
            @Parameter(hidden = true) HttpServletRequest request, // ✅ Lấy request để trích xuất token xác định user
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Incident creation request", required = true
            )
            @Valid @RequestBody CreateReportRequest body // ✅ Request body chứa thông tin sự cố cần báo cáo (có validate)
    ) {
        // 🟢 Lấy userId của nhân viên hiện tại từ token trong request
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🟢 Gọi service để tạo mới một báo cáo sự cố, kèm theo ID của người tạo
        ReportResponse response = reportService.createIncident(userId, body);

        // 🟢 Trả về HTTP 201 (CREATED) cùng thông tin chi tiết của báo cáo vừa tạo
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ✅ 2. ADMIN: CẬP NHẬT TRẠNG THÁI BÁO CÁO SỰ CỐ
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ quản trị viên mới có quyền cập nhật trạng thái sự cố
    @PostMapping("/{incidentId}/status") // 🔗 Endpoint: POST /api/incidents/{incidentId}/status?status=RESOLVED
    @Operation(
            summary = "Update incident status", // 📝 Swagger mô tả ngắn
            description = "Update the status of an incident report" // 📝 Mô tả chi tiết
    )
    public ResponseEntity<Void> updateIncidentStatus(
            @Parameter(description = "Incident ID") @PathVariable Long incidentId, // ✅ ID của báo cáo sự cố
            @Parameter(description = "New status") @RequestParam String status // ✅ Trạng thái mới (ví dụ: OPEN, RESOLVED, CANCELED,...)
    ) {
        // 🟢 Gọi service để cập nhật trạng thái báo cáo sự cố
        reportService.updateIncidentStatus(incidentId, status);

        // 🟢 Trả về HTTP 200 OK (thành công, không cần trả dữ liệu)
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // ✅ 3. ADMIN / STAFF: LẤY DANH SÁCH TẤT CẢ BÁO CÁO SỰ CỐ
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 Cả ADMIN và STAFF đều có quyền xem danh sách sự cố
    @GetMapping // 🔗 Endpoint: GET /api/incidents
    @Operation(
            summary = "Get all incidents", // 📝 Mô tả ngắn cho Swagger
            description = "Get list of all incidents (admin/staff tool)" // 📝 Chi tiết: cho phép admin/staff xem tất cả báo cáo
    )
    public ResponseEntity<List<ReportResponse>> getIncidents() {
        // 🟢 Gọi service để lấy danh sách tất cả báo cáo sự cố trong hệ thống
        List<ReportResponse> incidents = reportService.findAll();

        // 🟢 Trả về HTTP 200 OK cùng danh sách báo cáo
        return ResponseEntity.ok(incidents);
    }

}
