package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.TripletStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverViolationTripletResponse {
    private Long tripletId;

    private Long driverId;
    private String driverName;
    private String phoneNumber;

    private int countInGroup;
    private double totalPenalty;
    private TripletStatus status;

    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
