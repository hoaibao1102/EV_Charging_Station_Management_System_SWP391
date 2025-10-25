package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
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
    private ChargingStationStatus status;
    private LocalDateTime createdAt;
}
