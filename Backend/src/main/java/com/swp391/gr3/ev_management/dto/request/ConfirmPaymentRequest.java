package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.PaymentType;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private Long staffId;
    private Long invoiceId;
    private PaymentType paymentMethod;
    private String status;
    private double amount;
}
