package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.DTO.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.ConnectorTypeResponse;

import java.util.List;

public interface ConnectorTypeService {

    List<ConnectorTypeResponse> getAllConnectorTypes();

    ConnectorTypeResponse getConnectorTypeById(int connectorTypeId);

    ConnectorTypeResponse createConnectorType(ConnectorTypeCreateRequest request);

    ConnectorTypeResponse updateConnectorType(int connectorTypeId, ConnectorTypeUpdateRequest request);

}
