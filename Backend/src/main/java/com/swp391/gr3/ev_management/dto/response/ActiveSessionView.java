package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActiveSessionView {
    private Long sessionId;
    private String connector;      // hiển thị displayName hoặc code
    private String pointNumber;
    private String stationName;
    private String driverName;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // dùng scheduledEndTime (hoặc COALESCE ở service)
}
