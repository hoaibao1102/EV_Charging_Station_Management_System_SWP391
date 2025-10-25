package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChargingPointMapper {

    public ChargingPointResponse toResponse(ChargingPoint p) {
        if (p == null) return null;

    // Entity already uses enum; default to AVAILABLE if null
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

    public List<ChargingPointResponse> toResponses(List<ChargingPoint> points) {
        return points == null ? List.of() : points.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
