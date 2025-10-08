package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

@Data
public class ViewCharSessionResponse {
    private Long sessionId;
    private Long bookingId;
    private String stationName;
    private String startTime;
    private String endTime;
    private double energyKWh;
    private int durationMinutes;
    private double cost;
    private String status;
}
