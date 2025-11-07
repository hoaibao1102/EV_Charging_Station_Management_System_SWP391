package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateVehicleRequest {
    @NotNull(message = "VehicleId cannot be null")
    private Long modelId;       // optional
    @NotNull(message = "VehicleId cannot be null")
    private String licensePlate; // optional
}
