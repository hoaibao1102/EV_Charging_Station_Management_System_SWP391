package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.service.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<PaymentMethod> create(@RequestParam PaymentType methodType,
                                                @RequestParam PaymentProvider provider,
                                                @RequestParam String accountNo,
                                                @RequestParam(required = false) LocalDate expiryDate) {
        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(methodType, provider, accountNo, expiryDate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethod> update(@PathVariable Long id,
                                                @RequestParam PaymentType methodType,
                                                @RequestParam PaymentProvider provider,
                                                @RequestParam String accountNo,
                                                @RequestParam(required = false) LocalDate expiryDate) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(id, methodType, provider, accountNo, expiryDate));
    }

    @GetMapping()
    public ResponseEntity<List<PaymentMethodResponse>> getAll() {
        return ResponseEntity.ok(paymentMethodService.getAllPaymentMethods());
    }
}
