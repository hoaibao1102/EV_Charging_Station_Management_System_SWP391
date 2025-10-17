package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;

public class DriverCreateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private DriverStatus driverStatus = DriverStatus.PENDING; // default

    private String driverName; // optional, hoặc lấy từ user
}
