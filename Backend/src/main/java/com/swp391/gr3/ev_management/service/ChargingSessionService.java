package com.swp391.gr3.ev_management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.ActiveSessionView;
import com.swp391.gr3.ev_management.dto.response.CompletedSessionView;
import com.swp391.gr3.ev_management.dto.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;

public interface ChargingSessionService {

    StartCharSessionResponse startChargingSession(StartCharSessionRequest request);

    StopCharSessionResponse stopChargingSession(StopCharSessionRequest request);

    ViewCharSessionResponse getCharSessionById(Long sessionId);

    List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId);

    List<ChargingSession> getAll();

    Optional<ChargingSession> findById(Long sessionId);

    StopCharSessionResponse driverStopSession(Long sessionId, Long requesterUserId, Integer finalSocFromRequest);

    StopCharSessionResponse staffStopSession(Long sessionId, Long requesterUserId);

    List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId);
    /**
     * ✅ Trả về danh sách phiên sạc theo pointId (điểm sạc).
     */
    List<ViewCharSessionResponse> getSessionsByPoint(Long pointId);

    List<ActiveSessionView> getActiveSessionsCompact(Long userId);

    List<CompletedSessionView> getCompletedSessionsCompactByStaff(Long userId);

    List<ChargingSession> findAllByDriverUserIdDeep(Long userId);

    double sumEnergyAll();

    long countAll();

    long countByStationBetween(Long stationId, LocalDateTime yearFrom, LocalDateTime yearTo);

    long countSessionsByUserId(Long userId);

    Optional<ChargingSession> findByBooking_BookingId(Long bookingId);

    long countByStatus(ChargingSessionStatus active);

    List<ChargingSession> findTop5ByOrderByStartTimeDesc();

    List<ChargingSession> findByStartTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
