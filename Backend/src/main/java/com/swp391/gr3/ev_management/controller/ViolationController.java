package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.ViolationRequest;
import com.swp391.gr3.ev_management.dto.response.ViolationResponse;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import com.swp391.gr3.ev_management.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu class này là REST Controller → tự động trả JSON cho client
@RequestMapping("/api/violations") // ✅ Định nghĩa tiền tố URL cho toàn bộ endpoint của controller
@RequiredArgsConstructor // ✅ Tự động inject các dependency qua constructor (Lombok)
@Slf4j // ✅ Dùng để log thông tin (log.info, log.error,...)
@Tag(name = "Violation", description = "APIs for managing violation") // ✅ Ghi chú mô tả cho Swagger
public class ViolationController {

    private final ViolationService violationService; // ✅ Service chứa toàn bộ nghiệp vụ xử lý vi phạm

    /**
     * ✅ Tạo một vi phạm mới cho driver
     * POST /api/violations/users/{userId}
     * - Admin hoặc Staff có quyền tạo.
     * - Nếu người dùng bị >= 3 vi phạm đang ACTIVE → tự động BAN tài xế.
     */
    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN' or 'STAFF')")  // 🔒 Chỉ ADMIN hoặc STAFF được phép tạo violation
    @Operation(
            summary = "Create a new violation for a driver",
            description = "Creates a new violation for the specified driver. Automatically bans the driver if they reach 3 active violations."
    )
    public ResponseEntity<ViolationResponse> createViolation(
            @PathVariable Long userId, // ✅ ID của người dùng (driver)
            @Valid @RequestBody ViolationRequest request // ✅ Dữ liệu vi phạm: loại lỗi, mô tả, thời gian, ...
    ) {

        log.info("Received request to create violation for userId: {}", userId);

        // 🟢 Gọi service để tạo violation mới cho tài xế có userId tương ứng
        ViolationResponse response = violationService.createViolation(userId, request);

        // 🟢 Nếu service xác định driver bị auto-ban (>= 3 vi phạm ACTIVE)
        HttpStatus status = response.isDriverAutoBanned()
                ? HttpStatus.CREATED // Vẫn trả 201 Created (dù có ban)
                : HttpStatus.CREATED; // Cả hai trường hợp đều trả CREATED — chỉ khác thông tin trong response

        return ResponseEntity.status(status).body(response); // ✅ Trả về thông tin violation vừa tạo
    }

    /**
     * ✅ Lấy danh sách tất cả vi phạm của một tài xế
     * GET /api/violations/users/{userId}
     * - Chỉ ADMIN có quyền xem toàn bộ.
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')") // 🔒 Chỉ ADMIN được phép xem
    @Operation(
            summary = "Get all violations for a driver",
            description = "Retrieves all violations associated with the specified driver."
    )
    public ResponseEntity<List<ViolationResponse>> getViolations(@PathVariable Long userId) {
        log.info("Getting all violations for userId: {}", userId);

        // 🟢 Gọi service để lấy danh sách tất cả vi phạm theo userId
        List<ViolationResponse> violations = violationService.getViolationsByUserId(userId);

        // 🟢 Trả về danh sách violation dạng JSON
        return ResponseEntity.ok(violations);
    }

    /**
     * ✅ Lấy danh sách vi phạm của tài xế theo trạng thái cụ thể
     * GET /api/violations/users/{userId}/status/{status}
     * - Ví dụ: ACTIVE / RESOLVED / CANCELED
     */
    @GetMapping("/users/{userId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN')") // 🔒 Chỉ ADMIN được phép xem
    @Operation(
            summary = "Get violations by status for a driver",
            description = "Retrieves violations for the specified driver filtered by violation status."
    )
    public ResponseEntity<List<ViolationResponse>> getViolationsByStatus(
            @PathVariable Long userId, // ✅ ID của driver
            @PathVariable ViolationStatus status // ✅ Trạng thái vi phạm (enum)
    ) {
        log.info("Getting violations for userId: {} with status: {}", userId, status);

        // 🟢 Gọi service để lấy danh sách violation theo userId và status
        List<ViolationResponse> violations = violationService.getViolationsByUserIdAndStatus(userId, status);

        return ResponseEntity.ok(violations);
    }

    /**
     * ✅ Đếm số vi phạm đang ACTIVE của một tài xế
     * GET /api/violations/users/{userId}/count
     * - Dùng để kiểm tra xem có cần ban tài xế hay chưa.
     */
    @GetMapping("/users/{userId}/count")
    @PreAuthorize("hasAnyRole('ADMIN')") // 🔒 Chỉ ADMIN được phép xem
    @Operation(
            summary = "Count active violations for a driver",
            description = "Counts the number of active violations for the specified driver."
    )
    public ResponseEntity<Integer> countActiveViolations(@PathVariable Long userId) {
        log.info("Counting active violations for userId: {}", userId);

        // 🟢 Gọi service để đếm số lượng violation có status = ACTIVE
        int count = violationService.countActiveViolations(userId);

        // 🟢 Trả về số lượng (vd: 2 -> tài xế có 2 vi phạm đang hoạt động)
        return ResponseEntity.ok(count);
    }
}
