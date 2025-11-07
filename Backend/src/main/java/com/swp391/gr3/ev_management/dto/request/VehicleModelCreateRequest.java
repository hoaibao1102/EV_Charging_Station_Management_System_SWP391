package com.swp391.gr3.ev_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelCreateRequest {
    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @Min(value = 1886, message = "Year must be realistic")
    private int year;

    @NotBlank
    private String imageUrl;

    @NotBlank
    private String imagePublicId;

    @NotNull(message = "connectorTypeId is required")
    private Long connectorTypeId;

    @NotNull(message = "Status is required")
    private VehicleModelStatus status;

    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @NotNull(message = "Battery Capacity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Battery Capacity must be greater than 0")
    private Double batteryCapacityKWh;

}
