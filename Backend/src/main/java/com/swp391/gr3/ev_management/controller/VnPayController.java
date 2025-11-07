package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.service.PaymentService;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final PaymentService paymentService;
    private final TokenService tokenService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> create(
            @RequestParam Long sessionId,
            @RequestParam Long paymentMethodId,
            HttpServletRequest request) throws Exception {

        // Lấy userId từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        String clientIp = getClientIp(request);

        String payUrl = paymentService.createVnPayPaymentUrl(
                userId, sessionId, paymentMethodId, clientIp
        );

        return ResponseEntity.ok(Collections.singletonMap("paymentUrl", payUrl));
    }

    // Controller
    @GetMapping("/return")
    public ResponseEntity<?> handleReturn(HttpServletRequest req) throws Exception {
        paymentService.handleVnPayReturn(req); // truyền request để đọc raw query
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
