package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.TransactionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class TransactionBriefResponse {
    private Long transactionId;
    private double amount;
    private String currency;
    private String description;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    private Long invoiceId;
    private Long sessionId;
    private Long bookingId;

    private Long stationId;
    private String stationName; // nếu có trường name trong ChargingStation (tuỳ entity của bạn)

    private Long vehicleId;
    private String vehiclePlate;
}
