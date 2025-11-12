package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConfirmedBookingView {
    private String connector;      // DisplayName hoặc code
    private String pointNumber;    // Số hiệu cổng sạc
    private String stationName;    // Tên trạm
    private String driverName;     // Tên tài xế
    private String vehiclePlate;   // Biển số xe
    private LocalDateTime startTime; // scheduledStartTime
    private LocalDateTime endTime;   // scheduledEndTime
}
