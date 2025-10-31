package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    @NotNull(message = "Staff ID cannot be null")
    private Long staffId;
    @NotNull(message = "Invoice ID cannot be null")
    private Long invoiceId;
    @NotNull(message = "Payment method cannot be null")
    private PaymentType paymentMethod;
    @NotNull(message = "Status cannot be null")
    private String status;
    private Long amount;
}
