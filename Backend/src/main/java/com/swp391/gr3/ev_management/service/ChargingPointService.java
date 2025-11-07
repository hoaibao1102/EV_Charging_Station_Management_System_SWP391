package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.dto.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingPointResponse;

import java.util.List;

public interface ChargingPointService {
    ChargingPointResponse stopChargingPoint(StopChargingPointRequest request);
    List<ChargingPointResponse> getAllPoints();
    List<ChargingPointResponse> getPointsByStationId(Long stationId);

    ChargingPointResponse createChargingPoint(CreateChargingPointRequest request);
}
