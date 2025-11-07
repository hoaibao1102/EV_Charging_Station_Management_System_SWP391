package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.service.SlotTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController // ✅ Đánh dấu đây là REST controller (trả dữ liệu JSON)
@RequestMapping("/api/slot-templates") // ✅ Tất cả endpoint trong controller bắt đầu bằng /api/slot-templates
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho field final (Dependency Injection)
@Tag(name = "Slot Template", description = "APIs for managing slot templates")
// ✅ Dùng cho Swagger: nhóm các API thuộc phần quản lý mẫu khung giờ (slot templates)
public class SlotTemplateController {

    private final SlotTemplateService slotTemplateService; // ✅ Service xử lý nghiệp vụ liên quan đến "slot templates"

    // =========================================================================
    // ✅ 1. ADMIN: SINH (GENERATE) MẪU KHUNG GIỜ (SLOT TEMPLATE)
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền sinh slot template
    @PostMapping("/generate") // 🔗 Endpoint: POST /api/slot-templates/generate
    @Operation(summary = "Generate slot templates for a given configuration and date range")
    // 📝 Swagger: mô tả API này giúp sinh mẫu khung giờ dựa theo cấu hình và khoảng thời gian
    public ResponseEntity<Void> generateTemplates(
            @RequestParam Long configId, // ✅ ID của cấu hình khung giờ (SlotConfig) được dùng để sinh slot
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate, // ✅ Ngày bắt đầu
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate // ✅ Ngày kết thúc
    ) {
        // 🟢 Gọi service để sinh ra các slot template hàng ngày theo khoảng thời gian
        // 🟢 Service này sẽ dựa trên cấu hình (configId) để tạo ra các slot tương ứng (ví dụ: 08:00–09:00, 09:00–10:00,...)
        slotTemplateService.generateDailyTemplates(configId, startDate, endDate);

        // 🟢 Trả về HTTP 200 OK (không có nội dung body)
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // ✅ 2. ADMIN: LẤY DANH SÁCH TẤT CẢ CÁC SLOT TEMPLATE
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN được phép xem toàn bộ danh sách
    @GetMapping // 🔗 Endpoint: GET /api/slot-templates
    @Operation(summary = "Get all slot configurations") // 📝 Swagger mô tả: Lấy danh sách tất cả template hiện có
    public ResponseEntity<List<SlotTemplateResponse>> getAll() {
        // 🟢 Gọi service để lấy tất cả slot templates trong hệ thống
        // 🟢 Mỗi template là một "mẫu khung giờ" (ví dụ: 1 slot sạc có thời gian bắt đầu - kết thúc, trạng thái, cấu hình liên kết, ...)
        return ResponseEntity.ok(slotTemplateService.getAll());
    }

    // =========================================================================
    // ✅ 3. PUBLIC: LẤY THÔNG TIN CHI TIẾT CỦA 1 SLOT TEMPLATE THEO ID
    // =========================================================================
    @GetMapping("{templateId}") // 🔗 Endpoint: GET /api/slot-templates/{templateId}
    @Operation(
            summary = "Get slot templates by configuration ID",
            description = "Retrieve all slot templates associated with a specific configuration ID"
    )
    public ResponseEntity<SlotTemplateResponse> getById(
            @PathVariable Long templateId // ✅ ID của slot template cần tìm
    ) {
        // 🟢 Gọi service để lấy thông tin chi tiết của slot template theo ID
        SlotTemplateResponse response = slotTemplateService.getById(templateId);

        // ❌ Nếu không tìm thấy slot template -> trả về HTTP 404 Not Found
        if (response == null) return ResponseEntity.notFound().build();

        // ✅ Nếu tìm thấy -> trả về HTTP 200 OK cùng dữ liệu slot template
        return ResponseEntity.ok(response);
    }
}
