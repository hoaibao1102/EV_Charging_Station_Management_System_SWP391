package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StartCharSessionResponse {
    private Long sessionId;
    private Long bookingId;
    private String stationName;
    private String pointNumber;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private String status;
}
