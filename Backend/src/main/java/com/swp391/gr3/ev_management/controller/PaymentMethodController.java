package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.service.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController // ✅ Đánh dấu đây là REST Controller (trả dữ liệu JSON thay vì view)
@RequestMapping("/api/payment-methods") // ✅ Prefix chung cho toàn bộ endpoint của controller
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService; // ✅ Service xử lý nghiệp vụ liên quan đến phương thức thanh toán

    // ✅ Constructor injection (không dùng @RequiredArgsConstructor ở đây)
    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    // =========================================================================
    // ✅ 1. ADMIN: TẠO MỚI PHƯƠNG THỨC THANH TOÁN
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ người có quyền ADMIN mới được tạo phương thức thanh toán
    @PostMapping() // 🔗 Endpoint: POST /api/payment-methods
    public ResponseEntity<PaymentMethodResponse> create(
            @RequestParam PaymentType methodType, // ✅ Kiểu thanh toán (VD: CREDIT_CARD, BANK_TRANSFER, ...), lấy từ query param
            @RequestParam PaymentProvider provider, // ✅ Nhà cung cấp (VD: VISA, MASTERCARD, MOMO,...)
            @RequestParam String accountNo, // ✅ Số tài khoản / số thẻ
            @RequestParam(required = false) LocalDate expiryDate // ✅ Ngày hết hạn (tuỳ chọn)
    ) {
        // 🟢 Gọi service để tạo phương thức thanh toán mới
        PaymentMethodResponse response = paymentMethodService.createPaymentMethod(methodType, provider, accountNo, expiryDate);

        // 🟢 Trả về HTTP 200 OK cùng dữ liệu phương thức thanh toán vừa tạo
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 2. ADMIN: CẬP NHẬT PHƯƠNG THỨC THANH TOÁN
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền chỉnh sửa
    @PutMapping("/{id}") // 🔗 Endpoint: PUT /api/payment-methods/{id}
    public ResponseEntity<PaymentMethodResponse> update(
            @PathVariable Long id, // ✅ ID của phương thức thanh toán cần cập nhật
            @RequestParam PaymentType methodType, // ✅ Kiểu thanh toán mới
            @RequestParam PaymentProvider provider, // ✅ Nhà cung cấp mới
            @RequestParam String accountNo, // ✅ Số tài khoản / thẻ mới
            @RequestParam(required = false) LocalDate expiryDate // ✅ Ngày hết hạn mới (có thể bỏ trống)
    ) {
        // 🟢 Gọi service để cập nhật thông tin phương thức thanh toán
        PaymentMethodResponse response = paymentMethodService.updatePaymentMethod(id, methodType, provider, accountNo, expiryDate);

        // 🟢 Trả về dữ liệu sau khi cập nhật (HTTP 200 OK)
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ✅ 3. CÔNG KHAI: LẤY TẤT CẢ PHƯƠNG THỨC THANH TOÁN
    // =========================================================================
    @GetMapping() // 🔗 Endpoint: GET /api/payment-methods
    public ResponseEntity<List<PaymentMethodResponse>> getAll() {
        // 🟢 Gọi service để lấy danh sách tất cả phương thức thanh toán (có thể dùng cho người dùng chọn khi thanh toán)
        List<PaymentMethodResponse> methods = paymentMethodService.getAllPaymentMethods();

        // 🟢 Trả về danh sách (HTTP 200 OK)
        return ResponseEntity.ok(methods);
    }
}
