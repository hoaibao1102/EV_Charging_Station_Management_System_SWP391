package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateIncidentRequest;
import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;

import java.util.List;

public interface StaffIncidentService {
    IncidentResponse createIncident(CreateIncidentRequest request);
    List<IncidentResponse> getIncidentsByStation(Long stationId, Long staffId);
    List<IncidentResponse> getUnresolvedIncidentsByStation(Long stationId, Long staffId);
    IncidentResponse getIncidentById(Long incidentId, Long staffId);
}
