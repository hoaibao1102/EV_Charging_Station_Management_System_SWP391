package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingStationResponse {
    private Long stationId;
    private String stationName;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingHours;
    private ChargingStationStatus status;
    private LocalDateTime createdAt;
}
