package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StartCharSessionResponse {
    private Long sessionId;
    private Long bookingId;
    private String stationName;
    private String pointNumber;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private ChargingSessionStatus status;
}
