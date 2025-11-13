package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.dto.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;

import java.util.List;
import java.util.Optional;

public interface ConnectorTypeService {

    List<ConnectorTypeResponse> getAllConnectorTypes();

    ConnectorTypeResponse getConnectorTypeById(Long connectorTypeId);

    ConnectorTypeResponse createConnectorType(ConnectorTypeCreateRequest request);

    ConnectorTypeResponse updateConnectorType(Long connectorTypeId, ConnectorTypeUpdateRequest request);

    Optional<ConnectorType> findById(Long connectorTypeId);

    List<ConnectorType> findAllById(List<Long> connectorTypeIds);

    List<ConnectorType> findDistinctByChargingPoints_Station_StationId(Long stationId);
}
