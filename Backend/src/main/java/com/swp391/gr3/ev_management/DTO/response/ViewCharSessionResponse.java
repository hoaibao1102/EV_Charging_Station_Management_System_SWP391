package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ViewCharSessionResponse {
    private Long sessionId;
    private Long bookingId;
    private Long driverId;
    private String stationName;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double energyKWh;
    private int durationMinutes;
    private double cost;
    private String currency;
    private String status;
}
