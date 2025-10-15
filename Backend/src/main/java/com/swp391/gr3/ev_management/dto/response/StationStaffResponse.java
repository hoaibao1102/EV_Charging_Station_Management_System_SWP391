package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StationStaffResponse {
    private Long stationStaffId;
    private Long stationId;
    private String userName;
    private String userEmail;
    private String userPhoneNumber;
    private String status;
    private LocalDateTime assignedAt;
}
