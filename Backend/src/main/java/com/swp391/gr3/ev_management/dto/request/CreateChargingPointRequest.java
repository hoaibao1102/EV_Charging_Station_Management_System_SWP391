package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Station ID cannot be null")
    private Long stationId;
    @NotNull(message = "Connector Type ID cannot be null")
    private Long connectorTypeId;
    @NotNull(message = "Point number cannot be null")
    private String pointNumber;
    @NotNull(message = "Serial number cannot be null")
    private String serialNumber;
    @NotNull(message = "Installation date cannot be null")
    private LocalDateTime installationDate;
    @NotNull(message = "Last maintenance date cannot be null")
    private LocalDateTime lastMaintenanceDate;
    @NotNull(message = "Max power (kW) cannot be null")
    private double maxPowerKW;
    @NotNull(message = "Status cannot be null")
    private ChargingPointStatus status;

    private LocalDateTime createdAt;

}
