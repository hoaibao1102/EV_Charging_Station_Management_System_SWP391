package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.dto.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;

import java.util.List;

public interface StaffPaymentService {
    ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request);
    List<UnpaidInvoiceResponse> getUnpaidInvoicesByStation(Long stationId, Long userId);
}
