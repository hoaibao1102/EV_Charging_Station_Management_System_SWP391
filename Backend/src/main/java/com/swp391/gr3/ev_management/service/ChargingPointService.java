package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.DTO.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.DTO.response.ChargingPointResponse;

import java.util.List;

public interface ChargingPointService {
    ChargingPointResponse stopChargingPoint(StopChargingPointRequest request);
    ChargingPointResponse getPointStatus(Long pointId, Long staffId);
    List<ChargingPointResponse> getPointsByStation(Long stationId, Long staffId);

    ChargingPointResponse createChargingPoint(CreateChargingPointRequest request);
}
