package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChargingPointRequest {
    private Long stationId;
    private Long connectorTypeId;
    private String pointNumber;
    private String serialNumber;
    private LocalDateTime installationDate;
    private LocalDateTime lastMaintenanceDate;
    private double maxPowerKW;
    private ChargingPointStatus status;
    private LocalDateTime createdAt;

}
