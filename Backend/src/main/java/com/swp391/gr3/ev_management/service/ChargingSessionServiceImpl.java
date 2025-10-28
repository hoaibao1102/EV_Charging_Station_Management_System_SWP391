package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.mapper.ChargingSessionMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ChargingSessionServiceImpl implements ChargingSessionService {

    private final ChargingSessionRepository sessionRepository;
    private final BookingsRepository bookingsRepository;
    private final ChargingSessionMapper mapper;
    private final NotificationsRepository notificationsRepository;
    private final SessionSocCache sessionSocCache;
    private final TaskScheduler taskScheduler;

    private final ChargingSessionTxHandler txHandler;              // ✅ bean TX
    private final ApplicationEventPublisher eventPublisher;        // ✅ publish mail event

    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        Booking booking = bookingsRepository
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new RuntimeException("Booking not found or not confirmed"));

        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = getBookingStart(booking);
        LocalDateTime windowEnd   = getBookingEnd(booking);

        if (now.isBefore(windowStart)) {
            throw new IllegalStateException("Chưa đến giờ đặt. Chỉ được bắt đầu từ: " + windowStart);
        }
        if (now.isAfter(windowEnd)) {
            throw new IllegalStateException("Đã quá giờ đặt (đến: " + windowEnd + "). Không thể bắt đầu.");
        }

        int initialSoc = ThreadLocalRandom.current().nextInt(5, 25);

        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        session.setInitialSoc(initialSoc);
        sessionRepository.save(session);

        sessionSocCache.put(session.getSessionId(), initialSoc);

        // (tuỳ bạn) chuyển sang IN_PROGRESS hoặc BOOKED; để nguyên như bạn đang dùng:
        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // schedule auto-stop — GỌI QUA BEAN TX HANDLER để có TX khi chạy
        Date triggerAt = Date.from(windowEnd.atZone(ZoneId.systemDefault()).toInstant());
        Long sid = session.getSessionId();
        taskScheduler.schedule(() -> txHandler.autoStopIfStillRunningTx(sid, windowEnd), triggerAt);

        // Notification + publish event
        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setBooking(booking);
        noti.setSession(session);
        noti.setTitle("Bắt đầu sạc #" + booking.getBookingId());
        noti.setContentNoti("Pin hiện tại: " + initialSoc + "%");
        noti.setType(NotificationTypes.CHARGING_STARTED);
        noti.setStatus(Notification.STATUS_UNREAD);
        noti.setCreatedAt(LocalDateTime.now());
        notificationsRepository.save(noti);
        eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId())); // ✅ để listener gửi mail

        return StartCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .bookingId(booking.getBookingId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .startTime(session.getStartTime())
                .status(session.getStatus())
                .initialSoc(initialSoc)
                .build();
    }

    @Override
    @Transactional
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {
        ChargingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        LocalDateTime endTime = LocalDateTime.now();
        Integer reqSoc = request.getFinalSoc();
        Integer latestSoc = (reqSoc != null) ? reqSoc : sessionSocCache.get(session.getSessionId()).orElse(null);

        // Delegate toàn bộ STOP qua bean TX (load lại bằng fetch-join bên trong)
        return txHandler.stopSessionInternalTx(session.getSessionId(), latestSoc, endTime);
    }

    @Transactional(readOnly = true)
    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Charging session not found"));
        return mapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId) {
        List<ChargingSession> active = sessionRepository.findActiveSessionsByStation(stationId);
        return active.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargingSession> getAll() {
        return sessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ChargingSession> findById(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    // ---- helpers (read-only) ----
    private LocalDateTime getBookingStart(Booking booking) {
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Booking has no slot start time"));
    }

    private LocalDateTime getBookingEnd(Booking booking) {
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Booking has no slot end time"));
    }
}
