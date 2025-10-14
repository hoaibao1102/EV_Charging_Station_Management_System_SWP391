package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;

import java.util.List;

public interface StaffCharSessionService {
    StartCharSessionResponse startChargingSession(StartCharSessionRequest request);
    StopCharSessionResponse stopChargingSession(StopCharSessionRequest request);
    ViewCharSessionResponse getCharSessionById(Long sessionId, Long staffId);
    List<ViewCharSessionResponse> getCharSessionsByStation(Long stationId, Long staffId);
    List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId, Long staffId);
}
