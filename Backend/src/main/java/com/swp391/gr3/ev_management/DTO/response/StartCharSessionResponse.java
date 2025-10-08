package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

@Data
public class StartCharSessionResponse {
    private Long sessionId;
    private String stationName;
    private String pointNumber;
    private String vehiclePlate;
    private String startTime;
    private String status;
}
