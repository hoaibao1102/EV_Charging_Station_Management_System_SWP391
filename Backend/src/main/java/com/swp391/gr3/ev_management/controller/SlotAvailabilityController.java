package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.service.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController // ✅ Đánh dấu đây là REST controller (trả JSON cho client)
@RequestMapping("/api/slot-availability") // ✅ Tất cả API trong controller này bắt đầu bằng /api/slot-availability
@RequiredArgsConstructor // ✅ Lombok tự sinh constructor cho field final (Dependency Injection)
@Tag(name = "Slot Availability", description = "APIs for managing slot availability")
// ✅ Dùng cho Swagger — nhóm các API thuộc phần quản lý khung giờ khả dụng
public class SlotAvailabilityController {

    private final SlotAvailabilityService slotAvailabilityService; // ✅ Service xử lý logic về slot availability (tình trạng khung giờ sạc)

    // =========================================================================
    // ✅ 1. ADMIN: CẬP NHẬT TRẠNG THÁI SLOT AVAILABILITY
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền thay đổi trạng thái slot
    @PatchMapping("/{slotAvailabilityId}/status") // 🔗 Endpoint: PATCH /api/slot-availability/{slotAvailabilityId}/status?status=AVAILABLE
    @Operation(summary = "Update slot availability status") // 📝 Swagger mô tả ngắn gọn chức năng
    public ResponseEntity<SlotAvailabilityResponse> updateStatus(
            @PathVariable Long slotAvailabilityId, // ✅ ID của slot cần cập nhật
            @RequestParam SlotStatus status // ✅ Trạng thái mới (ví dụ: AVAILABLE, BOOKED, UNAVAILABLE, ...)
    ) {
        // 🟢 Gọi service để cập nhật trạng thái của slot
        // 🟢 Trả về HTTP 200 cùng dữ liệu slot sau khi cập nhật
        return ResponseEntity.ok(slotAvailabilityService.updateStatus(slotAvailabilityId, status));
    }

    // =========================================================================
    // ✅ 2. ADMIN: LẤY TẤT CẢ CÁC CẤU HÌNH SLOT HIỆN CÓ
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền xem toàn bộ danh sách slot
    @GetMapping // 🔗 Endpoint: GET /api/slot-availability
    @Operation(summary = "Get all slot configurations") // 📝 Swagger mô tả
    public ResponseEntity<List<SlotAvailabilityResponse>> getAll() {
        // 🟢 Gọi service để lấy danh sách tất cả các slot availability trong hệ thống
        // 🟢 Trả về HTTP 200 OK cùng danh sách
        return ResponseEntity.ok(slotAvailabilityService.findAll());
    }

    // =========================================================================
    // ✅ 3. CÔNG KHAI (PUBLIC): LẤY DANH SÁCH SLOT THEO CHARGING POINT ID
    // =========================================================================
    @GetMapping("/{pointId}") // 🔗 Endpoint: GET /api/slot-availability/{pointId}
    @Operation(
            summary = "Get slot availability by pointId", // 📝 Tiêu đề API cho Swagger
            description = "Retrieve all slot availability records for a specific charging point" // 📝 Mô tả chi tiết
    )
    public ResponseEntity<List<SlotAvailabilityResponse>> getById(
            @PathVariable Long pointId // ✅ ID của trạm sạc (charging point)
    ) {
        // 🟢 Gọi service để lấy danh sách slot availability theo ID của trạm sạc
        List<SlotAvailabilityResponse> responses = slotAvailabilityService.findByPointId(pointId);

        if (responses.isEmpty()) {
            // ❌ Nếu không có slot nào -> trả về HTTP 404 cùng danh sách rỗng
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.emptyList());
        }

        // ✅ Nếu có dữ liệu -> trả về danh sách các slot (HTTP 200 OK)
        return ResponseEntity.ok(responses);
    }
}
