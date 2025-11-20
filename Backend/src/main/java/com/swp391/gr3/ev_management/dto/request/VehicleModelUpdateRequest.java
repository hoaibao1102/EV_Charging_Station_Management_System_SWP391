package com.swp391.gr3.ev_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelUpdateRequest {

    @NotBlank(message = "Vehicle model name cannot be blank")
    private String brand; // optional

    @NotBlank(message = "Vehicle model cannot be null")
    private String model; // optional

    @NotNull(message = "Year cannot be null")
    @Min(value = 1886, message = "Year must be realistic")
    @Positive(message = "Year must be positive")
    private Integer year; // optional

    @NotNull(message = "Fast charging support cannot be null")
    private String imageUrl; // optional

    @NotNull(message = "Fast charging support cannot be null")
    private String imagePublicId; // optional

    @NotNull(message = "Fast charging support cannot be null")
    @Positive(message = "Range must be positive")
    private Long connectorTypeId; // optional

    @NotNull(message = "Fast charging support cannot be null")
    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @DecimalMin(value = "0.0", inclusive = false, message = "Battery Capacity must be greater than 0")
    private Double batteryCapacityKWh; // optional
}

