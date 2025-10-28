package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.ViolationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationResponse {
    private Long violationId;
    private Long driverId;
    private Long userId;
    private String driverName;
    private ViolationStatus status;
    private String description;
    private double penaltyAmount;
    private String currency;
    private LocalDateTime occurredAt;
    private boolean driverAutoBanned;  // Có bị auto-ban sau violation này không
    private String message;  // Thông báo nếu bị ban
}
