package com.swp391.gr3.ev_management.DTO.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelUpdateRequest {
    private String brand; // optional
    private String model; // optional
    @Min(value = 1886, message = "Year must be realistic")
    private Integer year; // optional
    private String imageUrl; // optional
    private String imagePublicId; // optional
    private Long connectorTypeId; // optional
    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @DecimalMin(value = "0.0", inclusive = false, message = "Battery Capacity must be greater than 0")
    private Double batteryCapacityKWh; // optional
}

