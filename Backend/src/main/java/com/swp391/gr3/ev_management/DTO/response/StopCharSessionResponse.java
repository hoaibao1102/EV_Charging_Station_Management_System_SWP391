package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

@Data
public class StopCharSessionResponse {
    private Long sessionId;
    private String stationName;
    private String pointNumber;
    private String vehiclePlate;
    private String startTime;
    private String endTime;
    private double energyKWh;
    private double cost;
    private int durationMinutes;
    private String status;

}
