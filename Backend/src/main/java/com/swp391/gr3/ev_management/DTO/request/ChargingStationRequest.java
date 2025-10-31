package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Station name is required")
    private String stationName;
    @NotNull(message = "Address is required")
    private String address;
    @NotNull(message = "Latitude is required")
    private double latitude;
    @NotNull(message = "Longitude is required")
    private double longitude;
    @NotNull(message = "Operating hours are required")
    private String operatingHours;
    @NotNull(message = "Status is required")
    private ChargingStationStatus status;
    private LocalDateTime createdAt;
}
