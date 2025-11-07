package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.service.DriverViolationTripletService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // ✅ Đánh dấu đây là REST Controller (tự động trả về JSON)
@RequestMapping("/api/triplets") // ✅ Prefix chung cho các API: /api/triplets/...
@RequiredArgsConstructor // ✅ Lombok: tự sinh constructor cho field final (Dependency Injection)
public class DriverViolationTripletController {

    // ✅ Service xử lý nghiệp vụ liên quan đến “violation triplets” (bộ 3 vi phạm của tài xế)
    private final DriverViolationTripletService driverViolationTripletService;

    // =========================================================================
    // ✅ 1. ADMIN hoặc STAFF: LẤY TẤT CẢ CÁC BỘ 3 VI PHẠM (DRIVER VIOLATION TRIPLETS)
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 Chỉ ADMIN hoặc STAFF có quyền xem danh sách
    @GetMapping("/all") // 🔗 Endpoint: GET /api/triplets/all
    @Operation(summary = "Get all driver violation triplets") // 📝 Swagger: mô tả chức năng API
    public ResponseEntity<List<DriverViolationTripletResponse>> getAllTriplets() {
        // 🟢 Gọi service để lấy danh sách tất cả các bộ 3 vi phạm tài xế
        // (mỗi triplet có thể bao gồm thông tin driver, vi phạm, trạng thái, tiền phạt, ...)
        return ResponseEntity.ok(driverViolationTripletService.getAllTriplets());
    }

    // =========================================================================
    // ✅ 2. ADMIN hoặc STAFF: LẤY CÁC VI PHẠM CỦA DRIVER THEO SỐ ĐIỆN THOẠI
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 Chỉ ADMIN hoặc STAFF được truy cập
    @GetMapping("/by-phone") // 🔗 Endpoint: GET /api/triplets/by-phone?phoneNumber=...
    @Operation(summary = "Get driver violation triplets by user phone number") // 📝 Swagger mô tả API
    public ResponseEntity<List<DriverViolationTripletResponse>> getByPhone(
            @RequestParam String phoneNumber // ✅ Tham số truyền vào qua query param (ví dụ ?phoneNumber=0987654321)
    ) {
        // 🟢 Gọi service để tìm tất cả vi phạm liên quan đến tài xế có số điện thoại này
        return ResponseEntity.ok(driverViolationTripletService.getTripletsByUserPhone(phoneNumber));
    }

    // =========================================================================
    // ✅ 3. ADMIN hoặc STAFF: ĐÁNH DẤU VI PHẠM ĐÃ THANH TOÁN
    // =========================================================================
    @PutMapping("/{tripletId}/pay") // 🔗 Endpoint: PUT /api/triplets/{tripletId}/pay
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // 🔒 ADMIN và STAFF có thể cập nhật trạng thái thanh toán
    public ResponseEntity<DriverViolationTripletResponse> markTripletAsPaid(
            @PathVariable Long tripletId // ✅ ID của triplet cần cập nhật
    ) {
        // 🟢 Gọi service để đổi trạng thái của triplet thành “PAID” (đã thanh toán)
        // 🟢 Trả về phản hồi chứa thông tin triplet sau khi cập nhật
        return ResponseEntity.ok(driverViolationTripletService.updateTripletStatusToPaid(tripletId));
    }

    // =========================================================================
    // ✅ 4. CHỈ ADMIN: HỦY BỘ 3 VI PHẠM (CANCEL)
    // =========================================================================
    @PutMapping("/{tripletId}/cancel") // 🔗 Endpoint: PUT /api/triplets/{tripletId}/cancel
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền hủy triplet
    public ResponseEntity<DriverViolationTripletResponse> markTripletAsCancel(
            @PathVariable Long tripletId // ✅ ID triplet cần hủy
    ) {
        // 🟢 Gọi service để cập nhật trạng thái triplet thành “CANCELED” (đã bị hủy)
        return ResponseEntity.ok(driverViolationTripletService.updateTripletStatusToCanceled(tripletId));
    }
}