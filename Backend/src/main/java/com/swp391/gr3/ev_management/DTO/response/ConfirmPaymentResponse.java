package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConfirmPaymentResponse {
    private Long transactionId;
    private Long invoiceId;
    private Long sessionId;
    private Long staffId;
    private String staffName;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private double amount;
    private String currency;
    private String status;
    private LocalDateTime confirmedAt;
    private String message;
}
