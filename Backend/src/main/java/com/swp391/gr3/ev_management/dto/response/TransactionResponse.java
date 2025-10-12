package com.swp391.gr3.ev_management.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long transactionId;
    private Long driverId;
    private Long invoiceId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
