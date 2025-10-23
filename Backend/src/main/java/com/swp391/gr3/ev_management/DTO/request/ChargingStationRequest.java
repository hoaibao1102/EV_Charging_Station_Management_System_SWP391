package com.swp391.gr3.ev_management.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingStationRequest {
    private String stationName;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingHours;
    private String status;
    private LocalDateTime createdAt;
}
