package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopChargingPointRequest {

    @NotNull(message = "ID điểm sạc không được để trống")
    @Positive(message = "ID điểm sạc phải là số dương")
    private Long pointId;

    @NotNull(message = "Trạng thái mới không được để trống")
    private ChargingPointStatus newStatus;
}
