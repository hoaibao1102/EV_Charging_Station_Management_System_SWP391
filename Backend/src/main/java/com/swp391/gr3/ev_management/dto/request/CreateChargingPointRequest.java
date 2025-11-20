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

    @NotNull(message = "Station ID cannot be null")
    @Positive(message = "Station ID must be positive")
    private Long stationId;

    @NotNull(message = "Connector Type ID cannot be null")
    @Positive(message = "Connector Type ID must be positive")
    private Long connectorTypeId;

    @NotBlank(message = "Point number cannot be null")
    private String pointNumber;

    @NotBlank(message = "Serial number cannot be null")
    private String serialNumber;

    @NotNull(message = "Installation date cannot be null")
    private LocalDateTime installationDate;

    @NotNull(message = "Last maintenance date cannot be null")
    private LocalDateTime lastMaintenanceDate;

    @NotNull(message = "Max power (kW) cannot be null")
    private double maxPowerKW;

    @NotBlank(message = "Status cannot be null")
    private ChargingPointStatus status;

    private LocalDateTime createdAt;

}
