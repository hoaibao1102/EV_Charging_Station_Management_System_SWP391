package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private Long staffId;
    private String status;
    private Long transactionId;
}
