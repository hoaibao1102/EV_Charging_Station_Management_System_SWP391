package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Station name is required")
    private String stationName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Latitude is required")
    private double latitude;

    @NotBlank(message = "Longitude is required")
    private double longitude;

    @NotBlank(message = "Operating hours are required")
    private String operatingHours;

    @NotBlank(message = "Status is required")
    private ChargingStationStatus status;

    private LocalDateTime createdAt;
}
