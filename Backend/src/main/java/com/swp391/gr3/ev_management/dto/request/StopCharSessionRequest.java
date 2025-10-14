package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class StopCharSessionRequest {
    private Long sessionId;
    private Long staffId;
    private Long pointId;
    private double finalEnergyKWh;
}
