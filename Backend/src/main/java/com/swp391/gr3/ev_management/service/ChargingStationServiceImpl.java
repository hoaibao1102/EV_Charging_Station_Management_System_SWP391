package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingStationMapper;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChargingStationServiceImpl implements ChargingStationService {

    private final ChargingStationRepository chargingStationRepository;
    private final ChargingStationMapper chargingStationMapper;

    @Override
    public ChargingStationResponse findByStationId(long id) {
        ChargingStation station = chargingStationRepository.findByStationId(id);
        return chargingStationMapper.toResponse(station);
    }

    @Override
    public List<ChargingStationResponse> getAllStations() {
        return chargingStationRepository.findAll()
                .stream()
                .map(chargingStationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChargingStationResponse addChargingStation(ChargingStationRequest request) {
        ChargingStation station = chargingStationMapper.toEntity(request);
        if (station.getCreatedAt() == null) {
            station.setCreatedAt(LocalDateTime.now());
        }
        ChargingStation saved = chargingStationRepository.save(station);
        return chargingStationMapper.toResponse(saved);
    }

    @Override
    public ChargingStationResponse updateChargingStation(long id, ChargingStationRequest request) {
        ChargingStation existing = chargingStationRepository.findByStationId(id);
        if (existing == null) return null;

        chargingStationMapper.updateEntity(existing, request);
        ChargingStation updated = chargingStationRepository.save(existing);
        return chargingStationMapper.toResponse(updated);
    }

    @Override
    public ChargingStationResponse updateStationStatus(long stationId, ChargingStationStatus newStatus) {
        ChargingStation station = chargingStationRepository.findByStationId(stationId);
        if (station == null) {
            throw new ErrorException("Station not found with id: " + stationId);
        }

        // ⚡ cập nhật trạng thái
        station.setStatus(newStatus);
        station.setUpdatedAt(LocalDateTime.now());

        ChargingStation updated = chargingStationRepository.save(station);
        return chargingStationMapper.toResponse(updated);
    }
}