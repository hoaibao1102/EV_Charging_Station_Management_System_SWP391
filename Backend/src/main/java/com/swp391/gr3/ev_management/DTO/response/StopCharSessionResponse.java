package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StopCharSessionResponse {
    private Long sessionId;
    private String stationName;
    private String pointNumber;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double energyKWh;
    private double cost;
    private int durationMinutes;
    private String status;

}
