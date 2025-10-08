package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConfirmPaymentResponse {
    private Long transactionId;
    private double amount;
    private String currency;
    private String status;
    private LocalDateTime confirmedAt;
}
