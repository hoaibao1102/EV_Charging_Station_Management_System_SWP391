package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.entity.DriverStatus;
import com.swp391.gr3.ev_management.entity.User;
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
    private DriverStatus driverStatus =  DriverStatus.PENDING; // Initial status when creating a driver

    private long driverId;

    private User user;

    private String driverName;
}