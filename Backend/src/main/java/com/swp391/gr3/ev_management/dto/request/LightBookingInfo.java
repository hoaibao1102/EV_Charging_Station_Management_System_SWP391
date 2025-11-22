package com.swp391.gr3.ev_management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LightBookingInfo {

    private Long bookingId;          // ID đặt lịch
    private LocalDateTime start;     // Thời gian bắt đầu
    private LocalDateTime end;       // Thời gian kết thúc
    private Long vehicleId;          // ID xe
}
