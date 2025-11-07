package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.dto.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingPointMapper;
import com.swp391.gr3.ev_management.repository.ChargingPointRepository;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingPointServiceImpl implements ChargingPointService {

        private final ChargingPointRepository pointRepository;
        private final ChargingPointMapper chargingPointMapper;
        private final ChargingStationRepository chargingStationRepository;
        private final ConnectorTypeRepository connectorTypeRepository;

    @Override
    @Transactional
    public ChargingPointResponse stopChargingPoint(StopChargingPointRequest request) {

        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new ErrorException("Charging point not found"));

                if (point.getStatus() == ChargingPointStatus.OCCUPIED) {
            throw new RuntimeException("Cannot stop point while in use");
        }

        point.setStatus(request.getNewStatus());
        point.setUpdatedAt(LocalDateTime.now());
        pointRepository.save(point);

        return chargingPointMapper.toResponse(point);
    }

    @Override
    public List<ChargingPointResponse> getAllPoints() {
        return pointRepository.findAll()
                .stream()
                .map(chargingPointMapper::toResponse)
                .toList();
    }

    @Override
    public List<ChargingPointResponse> getPointsByStationId(Long stationId) {
        log.info("Fetching charging points for stationId={}", stationId);

        // 1) Lấy danh sách các charging point theo stationId
        List<ChargingPoint> points = pointRepository.findByStation_StationId(stationId);

        if (points.isEmpty()) {
            log.warn("No charging points found for stationId={}", stationId);
            return List.of();
        }

        // 2) Map sang DTO bằng mapper sẵn có
        return chargingPointMapper.toResponses(points);
    }

    @Override
    @Transactional
    public ChargingPointResponse createChargingPoint(CreateChargingPointRequest request) {
        // 1) Kiểm tra Station
        var station = chargingStationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ErrorException("Station not found"));

        // 2) Kiểm tra ConnectorType
        var connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Connector type not found"));

        // ❗ Chặn nếu connector type/station bị deprecated
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot create charging point: connector type is deprecated");
        }

        // 3) Kiểm tra trùng
        if (pointRepository.findByStation_StationIdAndPointNumber(
                request.getStationId(), request.getPointNumber()).isPresent()) {
            throw new ErrorException("Point number already exists in this station");
        }
        if (pointRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new ErrorException("Serial number already exists");
        }

        // 4) Map & save
        ChargingPoint point = chargingPointMapper.toEntity(request, station, connectorType);
        pointRepository.save(point);

        // 5) Trả response
        return chargingPointMapper.toResponse(point);
    }
}
