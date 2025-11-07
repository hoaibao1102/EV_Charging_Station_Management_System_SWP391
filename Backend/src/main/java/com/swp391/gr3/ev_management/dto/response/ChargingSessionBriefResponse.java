package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class ChargingSessionBriefResponse {
    private Long sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer initialSoc;
    private Integer finalSoc;
    private double energyKWh;
    private int durationMinutes;
    private double cost;
    private ChargingSessionStatus status;
    private LocalDateTime createdAt;

    private Long bookingId;
    private Long stationId;
    private String stationName; // nếu có
    private Long vehicleId;
    private String vehiclePlate;

    private Long invoiceId; // nếu có hoá đơn đi kèm
}