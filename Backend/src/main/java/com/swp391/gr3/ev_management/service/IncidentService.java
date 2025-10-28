package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateIncidentRequest;
import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;

import java.util.List;

public interface IncidentService {
    IncidentResponse createIncident(CreateIncidentRequest request);
    IncidentResponse findById(Long incidentId);
    List<IncidentResponse> findAll();
    void updateIncidentStatus(Long incidentId, String status);
}
