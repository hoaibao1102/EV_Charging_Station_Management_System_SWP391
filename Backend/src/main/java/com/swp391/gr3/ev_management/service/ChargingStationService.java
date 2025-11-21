package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public interface ChargingStationService {

    ChargingStationResponse findByStationId(long id);

    ChargingStationResponse addChargingStation(ChargingStationRequest request);

    ChargingStationResponse updateChargingStation(long id, ChargingStationRequest request);

    List<ChargingStationResponse> getAllStations();

    ChargingStationResponse updateStationStatus(long stationId, ChargingStationStatus newStatus);

    Optional<ChargingStation> findById(Long stationId);

    ChargingStation findEntityById(Long stationId);

    List<ChargingStation> findAll();

    long countByStatus(ChargingStationStatus active);
}
