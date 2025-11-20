package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopChargingPointRequest {

    @NotNull(message = "pointId is required")
    @Positive(message = "pointId must be positive")
    private Long pointId;

    @NotBlank(message = "newStatus is required")
    private ChargingPointStatus newStatus;
}
