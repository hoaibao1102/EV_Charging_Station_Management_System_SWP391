package com.swp391.gr3.ev_management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LightBookingInfo {
    private Long bookingId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Long vehicleId;
}