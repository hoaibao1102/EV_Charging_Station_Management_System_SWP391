package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChargingPointRequest {

    @NotNull(message = "ID trạm sạc không được để trống")
    @Positive(message = "ID trạm sạc phải là số dương")
    private Long stationId;

    @NotNull(message = "ID loại đầu nối không được để trống")
    @Positive(message = "ID loại đầu nối phải là số dương")
    private Long connectorTypeId;

    @NotBlank(message = "Số điểm sạc không được để trống")
    private String pointNumber;

    @NotBlank(message = "Số serial không được để trống")
    private String serialNumber;

    @NotNull(message = "Ngày lắp đặt không được để trống")
    private LocalDateTime installationDate;

    @NotNull(message = "Ngày bảo trì gần nhất không được để trống")
    private LocalDateTime lastMaintenanceDate;

    @NotNull(message = "Công suất tối đa (kW) không được để trống")
    private Double maxPowerKW;

    @NotNull(message = "Trạng thái không được để trống")
    private ChargingPointStatus status;

    private LocalDateTime createdAt;
}
