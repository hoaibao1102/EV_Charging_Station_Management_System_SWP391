package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StationStatusResponse {
    private Long stationId;
    private String name;

    private long totalPoints;
    private long availablePoints;
    private long inUsePoints;
    private long maintenancePoints;

    private String status;
}
