package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import lombok.Data;

@Data
public class StopChargingPointRequest {
    private Long pointId;
    private ChargingPointStatus newStatus;
}
