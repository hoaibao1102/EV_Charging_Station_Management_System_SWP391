package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChargingStationService {
    ChargingStationResponse findByStationId(long id);
    ChargingStationResponse addChargingStation(ChargingStationRequest request);
    ChargingStationResponse updateChargingStation(long id, ChargingStationRequest request);
    List<ChargingStationResponse> getAllStations();
    ChargingStationResponse updateStationStatus(long stationId, ChargingStationStatus newStatus);
}
