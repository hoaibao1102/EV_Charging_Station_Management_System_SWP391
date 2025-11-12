package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.*;
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
    StopCharSessionResponse staffStopSession(Long sessionId, Long requesterUserId);
    List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId);
    /**
     * ✅ Trả về danh sách phiên sạc theo pointId (điểm sạc).
     */
    List<ViewCharSessionResponse> getSessionsByPoint(Long pointId);
    List<ActiveSessionView> getActiveSessionsCompact(Long userId);
    List<CompletedSessionView> getCompletedSessionsCompactByStaff(Long userId);
}
