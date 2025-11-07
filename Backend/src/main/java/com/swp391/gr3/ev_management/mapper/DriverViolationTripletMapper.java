package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.entity.DriverViolationTriplet;
import org.springframework.stereotype.Component;

@Component
public class DriverViolationTripletMapper {

    public DriverViolationTripletResponse toResponse(DriverViolationTriplet t) {
        if (t == null) return null;

        var driver = t.getDriver();
        var user = (driver != null) ? driver.getUser() : null;

        return DriverViolationTripletResponse.builder()
                .tripletId(t.getTripletId())
                .driverId(driver != null ? driver.getDriverId() : null)
                .driverName(user != null ? user.getName() : "Unknown")
                .phoneNumber(user != null ? user.getPhoneNumber() : "N/A")
                .countInGroup(t.getCountInGroup())
                .totalPenalty(t.getTotalPenalty())
                .status(t.getStatus())
                .windowStartAt(t.getWindowStartAt())
                .windowEndAt(t.getWindowEndAt())
                .createdAt(t.getCreatedAt())
                .closedAt(t.getClosedAt())
                .build();
    }
}
