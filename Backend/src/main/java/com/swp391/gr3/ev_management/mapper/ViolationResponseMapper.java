package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.DriverViolation;
import org.springframework.stereotype.Component;

@Component
public class ViolationResponseMapper {

    /** Map 1 violation + cờ autoBanned -> ViolationResponse */
    public ViolationResponse toResponse(DriverViolation violation, boolean wasAutoBanned) {
        if (violation == null) return null;

        var driver = violation.getDriver();

        return ViolationResponse.builder()
                .violationId(violation.getViolationId())
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .driverName(driver.getUser().getName())
                .status(violation.getStatus())
                .description(violation.getDescription())
                .occurredAt(violation.getOccurredAt())
                .driverAutoBanned(wasAutoBanned)
                .message(wasAutoBanned
                        ? "Driver has been AUTO-BANNED due to 3 or more violations"
                        : null)
                .build();
    }
}
