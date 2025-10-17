package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StopPointRequest;
import com.swp391.gr3.ev_management.DTO.response.StopPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.repository.ChargingPointRepository;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffPointServiceImpl implements StaffPointService{

    private final ChargingPointRepository pointRepository;
    private final StationStaffRepository staffRepository;

    @Override
    @Transactional
    public StopPointResponse stopChargingPoint(StopPointRequest request) {
        StationStaff staff = staffRepository.findActiveByStationStaffId(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new RuntimeException("Charging point not found"));

        if (!staff.getStation().getStationId().equals(point.getStation().getStationId())) {
            throw new RuntimeException("No permission to manage this charging point");
        }

        if ("in_use".equalsIgnoreCase(point.getStatus())) {
            throw new RuntimeException("Cannot stop point while in use");
        }

        point.setStatus(request.getNewStatus());
        point.setUpdatedAt(LocalDateTime.now());
        pointRepository.save(point);

        return StopPointResponse.builder()
                .pointId(point.getPointId())
                .stationName(point.getStation().getStationName())
                .pointNumber(point.getPointNumber())
                .status(point.getStatus())
                .updatedAt(point.getUpdatedAt())
                .build();
    }

    @Override
    public StopPointResponse getPointStatus(Long pointId, Long staffId) {
        ChargingPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new RuntimeException("Point not found"));
        StationStaff staff = staffRepository.findActiveByStationStaffId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(point.getStation().getStationId())) {
            throw new RuntimeException("Staff has no permission for this point");
        }

        return StopPointResponse.builder()
                .pointId(point.getPointId())
                .stationName(point.getStation().getStationName())
                .status(point.getStatus())
                .build();
    }

    @Override
    public List<StopPointResponse> getPointsByStation(Long stationId, Long staffId) {
        StationStaff staff = staffRepository.findActiveByStationStaffId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        return pointRepository.findByStation_StationId(stationId)
                .stream()
                .map(p -> StopPointResponse.builder()
                        .pointId(p.getPointId())
                        .stationName(p.getStation().getStationName())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
