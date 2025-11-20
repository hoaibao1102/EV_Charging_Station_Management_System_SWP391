package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateVehicleRequest {

    @NotNull(message = "VehicleId cannot be null")
    @Positive(message = "VehicleId must be positive")
    private Long modelId;       // optional

    @NotBlank(message = "VehicleId cannot be null")
    @Pattern(
            regexp = "^\\d{2}[A-Za-z]{1,2}\\d{4,5}$",
            message = "License plate must follow VN structure: 2 numbers (province) + 1-2 letters + 4-5 numbers (EX: 86B381052, 30G12345, 51AB12345)"
    )
    private String licensePlate; // optional
}
