package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    @NotNull(message = "Staff ID cannot be null")
    @Positive(message = "Staff ID must be positive")
    private Long staffId;

    @NotNull(message = "Invoice ID cannot be null")
    @Positive(message = "Invoice ID must be positive")
    private Long invoiceId;

    @NotBlank(message = "Payment method cannot be null")
    private PaymentType paymentMethod;

    @NotBlank(message = "Status cannot be null")
    private String status;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Long amount;
}
