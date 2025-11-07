package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentResponse {
    private Long transactionId;
    private Long invoiceId;
    private Long sessionId;
    private Long staffId;
    private String staffName;
    private PaymentType paymentMethod;
    private LocalDateTime paidAt;
    private double amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime confirmedAt;
    private String message;
}
