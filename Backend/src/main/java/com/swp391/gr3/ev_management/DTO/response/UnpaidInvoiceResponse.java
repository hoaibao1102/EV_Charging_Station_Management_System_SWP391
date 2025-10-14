package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UnpaidInvoiceResponse {
    private Long invoiceId;
    private Long sessionId;
    private double amount;
    private String currency;
    private String status;
    private LocalDateTime issuedAt;
    private String stationName;
    private String driverName;
    private String vehiclePlate;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private LocalDateTime createdAt;
}
