package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> create(
            @RequestParam Long driverId,
            @RequestParam Long sessionId,
            @RequestParam Long paymentMethodId,
            @RequestParam String currency,      // "VND"
            @RequestParam double amount,
            HttpServletRequest request) throws Exception {

        String clientIp = getClientIp(request);

        String payUrl = paymentService.createVnPayPaymentUrl(
                driverId, sessionId, paymentMethodId, currency, amount, clientIp
        );

        return ResponseEntity.ok(Collections.singletonMap("paymentUrl", payUrl));
    }

    @GetMapping("/return")
    public ResponseEntity<String> handleReturn(HttpServletRequest request) throws Exception {
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, String> flat = new HashMap<>();
        paramMap.forEach((k, v) -> flat.put(k, (v != null && v.length > 0) ? v[0] : null));

        paymentService.handleVnPayReturn(flat);
        return ResponseEntity.ok("OK");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
