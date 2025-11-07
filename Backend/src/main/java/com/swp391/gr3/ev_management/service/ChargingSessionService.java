package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.ChargingSession;

import java.util.List;
import java.util.Optional;

public interface ChargingSessionService {
    StartCharSessionResponse startChargingSession(StartCharSessionRequest request);
    StopCharSessionResponse stopChargingSession(StopCharSessionRequest request);

    ViewCharSessionResponse getCharSessionById(Long sessionId);
    List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId);
    List<ChargingSession> getAll();

    Optional<ChargingSession> findById(Long sessionId);
    StopCharSessionResponse driverStopSession(Long sessionId, Long requesterUserId);
    List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId);
}
