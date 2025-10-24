package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequest {
    @Enumerated(EnumType.STRING)
    @NotBlank(message = "Status is required")
    private DriverStatus driverStatus =  DriverStatus.ACTIVE; // Initial status when creating a driver

    private long driverId;

    private String driverName;
}