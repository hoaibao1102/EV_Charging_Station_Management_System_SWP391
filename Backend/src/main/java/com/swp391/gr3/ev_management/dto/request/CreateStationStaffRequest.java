package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateStationStaffRequest {
    private RegisterRequest user;   // thông tin user mới
    private Long stationId;       // trạm muốn gắn
    private LocalDateTime assignedAt; // nếu null, service sẽ tự set = now
}
