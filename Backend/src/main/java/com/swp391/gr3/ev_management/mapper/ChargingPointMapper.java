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

        LocalDateTime now = LocalDateTime.now();

        ChargingPoint point = new ChargingPoint();
        point.setStation(station);
        point.setConnectorType(connectorType);
        point.setPointNumber(request.getPointNumber());
        point.setSerialNumber(request.getSerialNumber());

        // ✅ default = thời điểm tạo nếu client không gửi
        point.setInstallationDate(
                request.getInstallationDate() != null ? request.getInstallationDate() : now
        );
        point.setLastMaintenanceDate(
                request.getLastMaintenanceDate() != null ? request.getLastMaintenanceDate() : now
        );

        // ✅ maxPowerKW tối thiểu 22.0 nếu không hợp lệ
        Double reqPower = request.getMaxPowerKW();
        point.setMaxPowerKW(reqPower != null && reqPower > 0 ? reqPower : 22.0);

        // ✅ status mặc định AVAILABLE nếu null
        point.setStatus(
                request.getStatus() != null ? request.getStatus() : ChargingPointStatus.AVAILABLE
        );

        point.setCreatedAt(now);
        point.setUpdatedAt(now);
        return point;
    }
}
