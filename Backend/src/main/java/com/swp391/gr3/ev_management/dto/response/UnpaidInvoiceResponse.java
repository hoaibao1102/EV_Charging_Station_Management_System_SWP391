package com.swp391.gr3.ev_management.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnpaidInvoiceResponse {

    private Long invoiceId;            // 1
    private double amount;             // 2
    private String currency;           // 3
    private LocalDateTime issuedAt;    // 4
    private Long sessionId;            // 5
    private Long bookingId;            // 6
    private Long stationId;            // 7
    private String stationName;        // 8
    private Long vehicleId;            // 9
    private String vehiclePlate;       // 10
    private LocalDateTime sessionStartTime; // 11
    private LocalDateTime sessionEndTime;   // 12
    private LocalDateTime createdAt;        // 13
}
