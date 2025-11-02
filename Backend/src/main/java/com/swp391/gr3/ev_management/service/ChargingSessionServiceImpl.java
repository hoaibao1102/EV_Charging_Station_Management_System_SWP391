package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.DTO.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingSessionMapper;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.repository.ChargingSessionRepository;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingSessionServiceImpl implements ChargingSessionService {

    private final ChargingSessionRepository sessionRepository;
    private final BookingsRepository bookingsRepository;
    private final ChargingSessionMapper mapper;
    private final NotificationsRepository notificationsRepository;
    private final SessionSocCache sessionSocCache;
    private final TaskScheduler taskScheduler;

    // TX handler chạy trong transaction riêng khi auto-stop/stop
    private final ChargingSessionTxHandler txHandler;
    private final ApplicationEventPublisher eventPublisher;

    // === DÙNG MÚI GIỜ VIỆT NAM CHO TOÀN BỘ LUỒNG ===
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        Booking booking = bookingsRepository
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ErrorException("Booking not found or not confirmed"));

        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // Lấy thời điểm hiện tại theo VN
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // Lấy khung thời gian session theo VN (ưu tiên scheduledStart/End)
        LocalDateTime windowStart = resolveWindowStart(booking);
        LocalDateTime windowEnd   = resolveWindowEnd(booking);

        if (now.isBefore(windowStart)) {
            throw new ErrorException("Chưa đến giờ đặt. Chỉ được bắt đầu từ: " + windowStart);
        }
        if (now.isAfter(windowEnd)) {
            throw new ErrorException("Đã quá giờ đặt (đến: " + windowEnd + "). Không thể bắt đầu.");
        }

        int initialSoc = ThreadLocalRandom.current().nextInt(5, 25);

        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        session.setInitialSoc(initialSoc);
        sessionRepository.save(session);

        // cache mốc SOC
        sessionSocCache.put(session.getSessionId(), initialSoc);

        // chuyển booking về trạng thái phù hợp trong khi sạc
        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // schedule auto-stop tại windowEnd (theo VN)
        Instant triggerInstant = windowEnd.atZone(TENANT_ZONE).toInstant();
        Date triggerAt = Date.from(triggerInstant);
        Long sid = session.getSessionId();

        log.info("[SCHEDULE STOP] sessionId={} bookingId={} triggerAt(VN)={} now(VN)={}",
                sid, booking.getBookingId(), triggerAt, Date.from(now.atZone(TENANT_ZONE).toInstant()));

        taskScheduler.schedule(() -> {
            try {
                txHandler.autoStopIfStillRunningTx(sid, windowEnd); // windowEnd theo VN
            } catch (Exception ex) {
                // Không để exception làm mất log/nuốt job
                log.error("[SCHEDULE STOP] Uncaught error for sessionId={} windowEnd(VN)={}: {}",
                        sid, windowEnd, ex.getMessage(), ex);
            }
        }, triggerAt);

        // Notification + publish event
        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setBooking(booking);
        noti.setSession(session);
        noti.setTitle("Bắt đầu sạc #" + booking.getBookingId());
        noti.setContentNoti("Pin hiện tại: " + initialSoc + "%");
        noti.setType(NotificationTypes.CHARGING_STARTED);
        noti.setStatus(Notification.STATUS_UNREAD);
        noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
        notificationsRepository.save(noti);
        eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

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
                .orElseThrow(() -> new ErrorException("Session not found"));

        LocalDateTime endTime = LocalDateTime.now(TENANT_ZONE);

        Integer cachedSoc = sessionSocCache.get(session.getSessionId()).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        return txHandler.stopSessionInternalTx(session.getSessionId(), finalSocIfAny, endTime);
    }

    @Override
    @Transactional
    public StopCharSessionResponse driverStopSession(Long sessionId, Long requesterUserId) {
        ChargingSession session = sessionRepository.findWithOwnerById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        Long ownerUserId = session.getBooking()
                .getVehicle()
                .getDriver()
                .getUser()
                .getUserId();

        if (!ownerUserId.equals(requesterUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of this session");
        }

        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        return txHandler.stopSessionInternalTx(sessionId, finalSocIfAny, LocalDateTime.now(TENANT_ZONE));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId) {
        List<ChargingSession> sessions =
                sessionRepository.findAllByBooking_Station_StationIdOrderByStartTimeDesc(stationId);
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ErrorException("Charging session not found"));
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
    /**
     * Ưu tiên dùng scheduledStart/End nếu có (đồng bộ với luồng Violation),
     * fallback về slot template nếu 2 field này null.
     * Các LocalDateTime ở đây được hiểu theo VN (TENANT_ZONE) khi convert sang Instant.
     */
    private LocalDateTime resolveWindowStart(Booking booking) {
        if (booking.getScheduledStartTime() != null) return booking.getScheduledStartTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot start time"));
    }

    private LocalDateTime resolveWindowEnd(Booking booking) {
        if (booking.getScheduledEndTime() != null) return booking.getScheduledEndTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot end time"));
    }
}
