package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.service.PaymentService;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController // ✅ Đánh dấu class này là REST Controller (tất cả response đều trả về JSON)
@RequestMapping("/api/payment/vnpay") // ✅ Định nghĩa prefix URL cho tất cả API trong controller này
@RequiredArgsConstructor // ✅ Lombok tự động tạo constructor chứa các dependency (paymentService, tokenService)
public class VnPayController {

    private final PaymentService paymentService; // ✅ Service xử lý logic thanh toán (VNPAY, EVM, cập nhật hóa đơn...)
    private final TokenService tokenService;     // ✅ Service để trích xuất thông tin người dùng (userId) từ JWT token

    @Value("${app.frontend.vnpay-success-url}")
    private String vnpaySuccessUrl;              // ✅ URL frontend để redirect khi thanh

    @Value("${app.frontend.vnpay-fail-url}")
    private String vnpayFailUrl;                 // ✅ URL frontend để redirect khi thanh toán thất bại

    /**
     * ✅ API: Tạo thanh toán
     * - Endpoint: POST /api/payment/vnpay/create
     * - Dùng để tạo đường dẫn thanh toán cho phiên sạc.
     *
     * 👉 Luồng hoạt động:
     * 1️⃣ Lấy userId từ token.
     * 2️⃣ Kiểm tra phương thức thanh toán:
     *    - Nếu là EVM → xử lý nội bộ và trả message thành công.
     *    - Nếu là VNPAY → tạo URL redirect đến cổng thanh toán VNPAY.
     * 3️⃣ Nếu không thuộc 2 loại trên → báo chưa hỗ trợ.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam Long sessionId,       // 🟢 ID của phiên sạc cần thanh toán
            @RequestParam Long paymentMethodId, // 🟢 ID của phương thức thanh toán (EVM hoặc VNPAY)
            HttpServletRequest request          // 🟢 Request gốc để lấy token + IP client
    ) throws Exception {

        // 🔹 1. Lấy userId từ token đăng nhập của người dùng
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🔹 2. Nếu là phương thức thanh toán nội bộ (EVM)
        // → Xử lý trực tiếp, cập nhật invoice & transaction, không cần redirect
        if (paymentService.isEvmMethod(paymentMethodId)) {
            String msg = paymentService.processEvmPayment(userId, sessionId, paymentMethodId);
            // 🟢 Trả về JSON thông báo thanh toán thành công
            return ResponseEntity.ok(Collections.singletonMap("message", msg));
        }

        // 🔹 3. Nếu là phương thức VNPAY
        // → Tạo URL redirect đến cổng VNPAY
        if (paymentService.isVnPayMethod(paymentMethodId)) {
            String clientIp = getClientIp(request); // lấy IP người dùng để gửi cho VNPAY
            String payUrl = paymentService.createVnPayPaymentUrl(userId, sessionId, paymentMethodId, clientIp);
            // 🟢 Trả về link redirect để frontend mở trang thanh toán VNPAY
            return ResponseEntity.ok(Collections.singletonMap("paymentUrl", payUrl));
        }

        // 🔹 4. Trường hợp khác (chưa được hỗ trợ)
        return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Unsupported payment method"));
    }

    /**
     * ✅ API: Xử lý phản hồi từ VNPAY
     * - Endpoint: GET /api/payment/vnpay/return
     * - Khi người dùng thanh toán xong, VNPAY sẽ redirect về URL này.
     *
     * 👉 Luồng hoạt động:
     * 1️⃣ Đọc các tham số trong query (vnp_ResponseCode, vnp_SecureHash, ...).
     * 2️⃣ Gọi service để xác thực và cập nhật hóa đơn + giao dịch.
     * 3️⃣ Trả về phản hồi JSON cho frontend.
     */
    @GetMapping("/return")
    public ResponseEntity<Void> handleReturn(HttpServletRequest req) {
        String redirectUrl;

        try {
            // Nếu xử lý không ném exception ⇒ coi như thành công
            paymentService.handleVnPayReturn(req);
            redirectUrl = vnpaySuccessUrl;
        } catch (Exception e) {
            // Nếu có lỗi trong quá trình xử lý ⇒ redirect sang trang fail
            redirectUrl = vnpayFailUrl;
        }

        return ResponseEntity.status(302) // 302 Found → redirect
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    /**
     * ✅ Hàm tiện ích: Lấy địa chỉ IP của client
     * - VNPAY yêu cầu IP người dùng khi khởi tạo giao dịch.
     * - Nếu request đi qua proxy (load balancer), IP thật nằm trong header `X-Forwarded-For`.
     */
    private String getClientIp(HttpServletRequest request) {
        // 🔹 Kiểm tra header X-Forwarded-For (trong trường hợp request đi qua proxy)
        String xff = request.getHeader("X-Forwarded-For");
        // 🔹 Nếu tồn tại, lấy IP đầu tiên trong chuỗi (IP gốc của người dùng)
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        // 🔹 Nếu không có header này, lấy IP trực tiếp từ request
        return request.getRemoteAddr();
    }
}
