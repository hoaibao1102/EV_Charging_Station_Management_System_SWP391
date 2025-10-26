package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.ChargingSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChargingSessionService {
    StartCharSessionResponse startChargingSession(StartCharSessionRequest request);
    StopCharSessionResponse stopChargingSession(StopCharSessionRequest request);

    ViewCharSessionResponse getCharSessionById(Long sessionId);
    List<ViewCharSessionResponse> getCharSessionsByStation(Long stationId);
    List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId);
    List<ChargingSession> getAll();

    Optional<ChargingSession> findById(Long sessionId);
}
