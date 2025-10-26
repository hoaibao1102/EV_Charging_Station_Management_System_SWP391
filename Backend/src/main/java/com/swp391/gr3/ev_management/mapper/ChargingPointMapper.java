package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChargingPointMapper {

    public ChargingPointResponse toResponse(ChargingPoint p) {
        if (p == null) return null;

        ChargingPointStatus statusEnum = p.getStatus() != null
                ? p.getStatus()
                : ChargingPointStatus.AVAILABLE;

        return ChargingPointResponse.builder()
                .pointId(p.getPointId())
                .stationId(p.getStation() != null ? p.getStation().getStationId() : null)
                .stationName(p.getStation() != null ? p.getStation().getStationName() : null)
                .pointNumber(p.getPointNumber())
                .status(statusEnum)
                .serialNumber(p.getSerialNumber())
                .installationDate(p.getInstallationDate())
                .lastMaintenanceDate(p.getLastMaintenanceDate())
                .maxPowerKW((int) Math.round(p.getMaxPowerKW()))
                .connectorType(p.getConnectorType() != null ? p.getConnectorType().getDisplayName() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public List<ChargingPointResponse> toResponses(List<ChargingPoint> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ChargingPoint toEntity(CreateChargingPointRequest request,
                                  ChargingStation station,
                                  ConnectorType connectorType) {
        ChargingPoint point = new ChargingPoint();
        point.setStation(station);
        point.setConnectorType(connectorType);
        point.setPointNumber(request.getPointNumber());
        point.setSerialNumber(request.getSerialNumber());
        point.setInstallationDate(request.getInstallationDate() != null
                ? request.getInstallationDate()
                : LocalDateTime.now());
        point.setLastMaintenanceDate(request.getLastMaintenanceDate());
        point.setMaxPowerKW(request.getMaxPowerKW() > 0 ? request.getMaxPowerKW() : 22.0);
        point.setStatus(request.getStatus());
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        return point;
    }

}
