package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.dto.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.service.SlotConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu class là REST Controller (tự động trả dữ liệu JSON)
@RequestMapping("/api/slot-configs") // ✅ Prefix chung cho các API endpoint: /api/slot-configs/...
@RequiredArgsConstructor // ✅ Lombok tự động tạo constructor cho các field final (Dependency Injection)
@Tag(name = "Slot Configuration", description = "APIs for managing slot configurations")
// ✅ Dùng cho Swagger: nhóm các API quản lý cấu hình khung giờ (slot config)
public class SlotConfigController {

    private final SlotConfigService slotConfigService; // ✅ Service chứa logic xử lý liên quan đến slot configuration

    // =========================================================================
    // ✅ 1. ADMIN: CẬP NHẬT MỘT SLOT CONFIG ĐÃ TỒN TẠI
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền chỉnh sửa cấu hình khung giờ
    @PutMapping("/{configId}") // 🔗 Endpoint: PUT /api/slot-configs/{configId}
    @Operation(summary = "Update an existing slot configuration") // 📝 Swagger mô tả API
    public ResponseEntity<SlotConfigResponse> update(
            @PathVariable Long configId, // ✅ ID của cấu hình cần cập nhật
            @RequestBody SlotConfigRequest req // ✅ Dữ liệu cập nhật gửi từ client
    ) {
        // 🟢 Gọi service để cập nhật cấu hình
        SlotConfigResponse updated = slotConfigService.updateSlotConfig(configId, req);

        // ❌ Nếu không tìm thấy config theo ID -> trả về 404 Not Found
        if (updated == null) return ResponseEntity.notFound().build();

        // ✅ Nếu cập nhật thành công -> trả về HTTP 200 + dữ liệu cấu hình đã cập nhật
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // ✅ 2. ADMIN: VÔ HIỆU HÓA (DEACTIVATE) MỘT SLOT CONFIG
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền vô hiệu hóa
    @PutMapping("/{configId}/deactivate") // 🔗 Endpoint: PUT /api/slot-configs/{configId}/deactivate
    public ResponseEntity<SlotConfigResponse> deactivateSlotConfig(
            @PathVariable Long configId // ✅ ID của slot config cần vô hiệu hóa
    ) {
        try {
            // 🟢 Gọi service để vô hiệu hóa cấu hình (thường là đặt trạng thái ACTIVE -> INACTIVE)
            SlotConfigResponse response = slotConfigService.deactivateConfig(configId);

            // ✅ Trả về HTTP 200 OK cùng dữ liệu đã được cập nhật
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // ❌ Nếu dữ liệu không hợp lệ -> trả về 400 Bad Request
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // ⚠️ Nếu lỗi hệ thống -> trả về 500 Internal Server Error
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================================
    // ✅ 3. ADMIN: THÊM MỚI MỘT SLOT CONFIG
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền thêm cấu hình mới
    @PostMapping() // 🔗 Endpoint: POST /api/slot-configs
    @Operation(summary = "Add a new slot configuration") // 📝 Swagger mô tả
    public ResponseEntity<SlotConfigResponse> add(
            @RequestBody SlotConfigRequest req // ✅ Dữ liệu cấu hình mới (số lượng slot, thời lượng, thời gian bắt đầu/kết thúc, ...)
    ) {
        // 🟢 Gọi service để thêm cấu hình mới
        SlotConfigResponse created = slotConfigService.addSlotConfig(req);

        // ✅ Trả về HTTP 200 OK cùng dữ liệu cấu hình mới
        return ResponseEntity.ok(created);
    }

    // =========================================================================
    // ✅ 4. ADMIN: LẤY DANH SÁCH TẤT CẢ SLOT CONFIGS
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN được phép xem danh sách
    @GetMapping // 🔗 Endpoint: GET /api/slot-configs
    @Operation(summary = "Get all slot configurations") // 📝 Swagger mô tả API
    public ResponseEntity<List<SlotConfigResponse>> getAll() {
        // 🟢 Lấy danh sách tất cả cấu hình khung giờ (slot config)
        List<SlotConfigResponse> list = slotConfigService.findAll();

        // ✅ Trả về danh sách cấu hình
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // ✅ 5. ADMIN: LẤY THÔNG TIN MỘT SLOT CONFIG THEO ID
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @GetMapping("/{configId}") // 🔗 Endpoint: GET /api/slot-configs/{configId}
    @Operation(summary = "Get slot configuration by ID") // 📝 Swagger mô tả
    public ResponseEntity<SlotConfigResponse> getById(
            @PathVariable Long configId // ✅ ID của cấu hình khung giờ cần lấy
    ) {
        // 🟢 Gọi service để tìm cấu hình theo ID
        SlotConfigResponse response = slotConfigService.findByConfigId(configId);

        // ❌ Nếu không có -> trả về 404
        if (response == null) return ResponseEntity.notFound().build();

        // ✅ Nếu có -> trả về dữ liệu cấu hình (HTTP 200 OK)
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 6. ADMIN: LẤY CẤU HÌNH KHUNG GIỜ THEO ID CỦA TRẠM SẠC
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN
    @GetMapping("/station/{stationId}") // 🔗 Endpoint: GET /api/slot-configs/station/{stationId}
    @Operation(summary = "Get slot configuration by Charging Station ID") // 📝 Swagger mô tả
    public ResponseEntity<SlotConfigResponse> getByStation(
            @PathVariable Long stationId // ✅ ID của trạm sạc
    ) {
        // 🟢 Gọi service để tìm cấu hình khung giờ của một trạm sạc cụ thể
        SlotConfigResponse response = slotConfigService.findByStation_StationId(stationId);

        // ❌ Nếu không có -> trả về HTTP 404
        if (response == null) return ResponseEntity.notFound().build();

        // ✅ Nếu có -> trả về cấu hình khung giờ (HTTP 200)
        return ResponseEntity.ok(response);
    }
}
