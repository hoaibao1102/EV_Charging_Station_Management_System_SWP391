package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingStationResponse {
    private String stationName;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingHours;
    private String status;
    private LocalDateTime createdAt;
}
