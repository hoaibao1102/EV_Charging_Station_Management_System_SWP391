package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.DTO.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.DTO.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.service.StaffPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/payments")
@RequiredArgsConstructor
@Tag(name = "Staff Payment", description = "APIs for staff to manage payment confirmations")
public class PaymentController {

    private final StaffPaymentService paymentService;

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment", description = "Staff confirms payment for a completed charging session")
    public ResponseEntity<ConfirmPaymentResponse> confirmPayment(
             @RequestBody ConfirmPaymentRequest request
    ) {
        ConfirmPaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unpaid")
    @Operation(summary = "Get unpaid invoices", description = "Get list of unpaid invoices at a station")
    public ResponseEntity<List<UnpaidInvoiceResponse>> getUnpaidInvoices(
            @Parameter(description = "Station ID") @RequestParam Long stationId,
            @Parameter(description = "Staff ID") @RequestParam Long staffId
    ) {
        List<UnpaidInvoiceResponse> invoices = paymentService.getUnpaidInvoicesByStation(stationId, staffId);
        return ResponseEntity.ok(invoices);
    }
}
