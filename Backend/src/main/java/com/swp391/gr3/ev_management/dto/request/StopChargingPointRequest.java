package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StopChargingPointRequest {
    @NotBlank(message = "pointId is required")
    private Long pointId;
    @NotBlank(message = "newStatus is required")
    private ChargingPointStatus newStatus;
}
