package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddVehicleRequest {

    @NotNull(message = "Model ID cannot be null")
    @Positive(message = "Model ID must be positive")
    private Long modelId;
    
    @NotBlank(message = "License plate number cannot be left blank")
    @Pattern(
        regexp = "^\\d{2}[A-Za-z]{1,2}\\d{4,5}$",
        message = "License plate must follow VN structure: 2 numbers (province) + 1-2 letters + 4-5 numbers (EX: 86B381052, 30G12345, 51AB12345)"
    )
    private String licensePlate;

    @NotBlank
    private UserVehicleStatus vehicleStatus;
}
