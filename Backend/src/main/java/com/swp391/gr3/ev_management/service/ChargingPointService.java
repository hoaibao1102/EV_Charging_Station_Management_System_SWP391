package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StopPointRequest;
import com.swp391.gr3.ev_management.DTO.response.StopPointResponse;

import java.util.List;

public interface ChargingPointService {
    StopPointResponse stopChargingPoint(StopPointRequest request);
    StopPointResponse getPointStatus(Long pointId, Long staffId);
    List<StopPointResponse> getPointsByStation(Long stationId, Long staffId);

}
