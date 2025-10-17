package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ConfirmPaymentRequest;
import com.swp391.gr3.ev_management.DTO.response.ConfirmPaymentResponse;
import com.swp391.gr3.ev_management.DTO.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Invoice;

import java.util.List;

public interface StaffPaymentService {
    ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request);
    List<UnpaidInvoiceResponse> getUnpaidInvoicesByStation(Long stationId, Long userId);
}
