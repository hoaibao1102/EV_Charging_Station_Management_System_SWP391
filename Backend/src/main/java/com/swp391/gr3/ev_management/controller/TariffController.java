package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.dto.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.TariffResponse;
import com.swp391.gr3.ev_management.service.TariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu đây là REST Controller (tự động trả JSON)
@RequestMapping("/api/tariffs") // ✅ Tất cả endpoint trong controller này bắt đầu bằng /api/tariffs
@Tag(name = "Tariff Controller", description = "APIs for managing tariffs")
// ✅ Swagger: nhóm các API liên quan đến quản lý biểu giá (tariff)
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho các field final (Dependency Injection)
public class TariffController {

    private final TariffService tariffService; // ✅ Service xử lý logic về biểu giá (tariff)

    // =========================================================================
    // ✅ 1. ADMIN: TẠO MỚI BIỂU GIÁ (TARIFF)
    // =========================================================================
    @PostMapping // 🔗 Endpoint: POST /api/tariffs
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ người có quyền ADMIN được phép tạo biểu giá
    @SecurityRequirement(name = "bearerAuth") // 🔐 Swagger yêu cầu xác thực bằng Bearer Token
    @Operation(summary = "Create new tariff", description = "Admin only - Create a new tariff")
    public ResponseEntity<TariffResponse> createTariff(
            @Valid @RequestBody TariffCreateRequest request // ✅ Body chứa dữ liệu để tạo biểu giá (tên, đơn giá, loại sạc,...)
    ) {
        // 🟢 Gọi service để tạo mới một biểu giá trong hệ thống
        TariffResponse response = tariffService.createTariff(request);

        // 🟢 Trả về HTTP 201 (Created) cùng dữ liệu biểu giá mới
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ✅ 2. ADMIN: CẬP NHẬT BIỂU GIÁ (TARIFF) ĐÃ CÓ
    // =========================================================================
    @PutMapping("/{tariffId}") // 🔗 Endpoint: PUT /api/tariffs/{tariffId}
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN được phép cập nhật biểu giá
    @SecurityRequirement(name = "bearerAuth") // 🔐 Yêu cầu xác thực Bearer Token
    @Operation(summary = "Update tariff", description = "Admin only - Update an existing tariff")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable long tariffId, // ✅ ID của biểu giá cần cập nhật
            @Valid @RequestBody TariffUpdateRequest request // ✅ Thông tin mới để cập nhật biểu giá (giá/khoảng thời gian mới,...)
    ) {
        // 🟢 Gọi service để cập nhật biểu giá dựa trên ID và dữ liệu mới
        TariffResponse response = tariffService.updateTariff(tariffId, request);

        // 🟢 Trả về HTTP 200 OK cùng dữ liệu biểu giá đã được cập nhật
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 3. PUBLIC: LẤY DANH SÁCH TẤT CẢ BIỂU GIÁ
    // =========================================================================
    @GetMapping // 🔗 Endpoint: GET /api/tariffs
    @Operation(summary = "Get all tariffs", description = "Public endpoint to retrieve all tariffs")
    public ResponseEntity<List<TariffResponse>> getAllTariffs() {
        // 🟢 Gọi service để lấy danh sách tất cả các biểu giá trong hệ thống
        List<TariffResponse> list = tariffService.getAllTariffs();

        // 🟢 Trả về HTTP 200 OK cùng danh sách biểu giá
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // ✅ 4. PUBLIC: LẤY THÔNG TIN CHI TIẾT MỘT BIỂU GIÁ THEO ID
    // =========================================================================
    @GetMapping("/{tariffId}") // 🔗 Endpoint: GET /api/tariffs/{tariffId}
    @Operation(summary = "Get tariff by ID", description = "Public endpoint to retrieve a specific tariff")
    public ResponseEntity<TariffResponse> getTariffById(
            @PathVariable long tariffId // ✅ ID của biểu giá cần lấy
    ) {
        // 🟢 Gọi service để lấy chi tiết biểu giá theo ID
        TariffResponse response = tariffService.getTariffById(tariffId);

        // 🟢 Trả về HTTP 200 OK cùng dữ liệu biểu giá
        return ResponseEntity.ok(response);
    }
}
