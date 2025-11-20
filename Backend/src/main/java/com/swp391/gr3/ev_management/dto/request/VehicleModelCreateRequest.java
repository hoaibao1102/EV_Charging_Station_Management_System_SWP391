package com.swp391.gr3.ev_management.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "Trim is required")
    @Min(value = 1886, message = "Year must be realistic")
    private int year;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotNull(message = "Image Public ID is required")
    @Positive(message = "Image Public ID must be positive")
    private String imagePublicId;

    @NotNull(message = "connectorTypeId is required")
    @Positive(message = "connectorTypeId must be positive")
    private Long connectorTypeId;

    @NotBlank(message = "Status is required")
    private VehicleModelStatus status;

    @JsonAlias({"batteryCapacity", "battery-capacity"})
    @NotNull(message = "Battery Capacity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Battery Capacity must be greater than 0")
    private Double batteryCapacityKWh;

}
