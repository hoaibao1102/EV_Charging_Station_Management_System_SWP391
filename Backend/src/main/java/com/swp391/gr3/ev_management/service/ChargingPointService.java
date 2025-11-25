package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.dto.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;

import java.util.List;
import java.util.Map;

public interface ChargingPointService {

    ChargingPointResponse stopChargingPoint(StopChargingPointRequest request);

    List<ChargingPointResponse> getAllPoints();

    List<ChargingPointResponse> getPointsByStationId(Long stationId);

    ChargingPointResponse createChargingPoint(CreateChargingPointRequest request);

    List<ChargingPoint> findByStation_StationId(Long stationId);

    List<ChargingPoint> findByStation_StationIdAndConnectorType_ConnectorTypeId(Long stationId, Long connectorTypeId);

    Map<String, Long> countGroupByStatus();

    ChargingPointResponse getPointById(Long pointId);

    ChargingPointResponse updateChargingPoint(Long pointId, CreateChargingPointRequest request);

    void deleteChargingPoint(Long pointId);
}
