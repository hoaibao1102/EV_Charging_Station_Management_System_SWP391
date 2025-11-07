package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateReportRequest;
import com.swp391.gr3.ev_management.dto.response.ReportResponse;

import java.util.List;

public interface ReportService {
    ReportResponse createIncident(Long userId, CreateReportRequest request);
    ReportResponse findById(Long incidentId);
    List<ReportResponse> findAll();
    void updateIncidentStatus(Long incidentId, String status);
}
