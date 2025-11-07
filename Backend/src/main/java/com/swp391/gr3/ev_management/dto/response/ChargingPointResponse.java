package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChargingPointResponse {
    private Long pointId;
    private Long stationId;
    private String pointNumber;
    private String stationName;
    private ChargingPointStatus status;
    private String serialNumber;
    private LocalDateTime installationDate;
    private LocalDateTime lastMaintenanceDate;
    private int maxPowerKW;
    private String connectorType;
    private LocalDateTime updatedAt;
    private  LocalDateTime createdAt;
}
