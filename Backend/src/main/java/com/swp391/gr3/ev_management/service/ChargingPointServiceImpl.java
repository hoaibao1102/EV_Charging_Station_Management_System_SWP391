package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.DTO.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.DTO.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.mapper.ChargingPointMapper;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.repository.ChargingPointRepository;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChargingPointServiceImpl implements ChargingPointService {

        private final ChargingPointRepository pointRepository;
        private final StationStaffRepository staffRepository;
        private final ChargingPointMapper chargingPointMapper;

    @Override
    @Transactional
    public ChargingPointResponse stopChargingPoint(StopChargingPointRequest request) {
        StationStaff staff = staffRepository.findActiveByStationStaffId(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));

        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new RuntimeException("Charging point not found"));

        if (!staff.getStation().getStationId().equals(point.getStation().getStationId())) {
            throw new RuntimeException("No permission to manage this charging point");
        }

                if (point.getStatus() == ChargingPointStatus.OCCUPIED) {
            throw new RuntimeException("Cannot stop point while in use");
        }

        point.setStatus(request.getNewStatus());
        point.setUpdatedAt(LocalDateTime.now());
        pointRepository.save(point);

                return chargingPointMapper.toResponse(point);
    }

    @Override
    public ChargingPointResponse getPointStatus(Long pointId, Long staffId) {
        ChargingPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new RuntimeException("Point not found"));
        StationStaff staff = staffRepository.findActiveByStationStaffId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(point.getStation().getStationId())) {
            throw new RuntimeException("Staff has no permission for this point");
        }

        return chargingPointMapper.toResponse(point);
    }

    @Override
    public List<ChargingPointResponse> getPointsByStation(Long stationId, Long staffId) {
        StationStaff staff = staffRepository.findActiveByStationStaffId(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found or not active"));
        if (!staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff has no permission for this station");
        }

        return chargingPointMapper.toResponses(pointRepository.findByStation_StationId(stationId));
    }

    @Override
    public BookingResponse createChargingPoint(CreateChargingPointRequest request) {
        return null;
    }
}
