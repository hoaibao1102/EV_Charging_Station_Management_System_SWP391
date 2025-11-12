package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.*;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.StopInitiator;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingSessionMapper;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.repository.ChargingSessionRepository;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
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

@Service // Đánh dấu class là Spring Service (chứa nghiệp vụ phiên sạc)
@RequiredArgsConstructor // Generate constructor cho các field final (DI)
@Slf4j // Cung cấp logger
public class ChargingSessionServiceImpl implements ChargingSessionService {

    // ====== Dependencies chính ======
    private final ChargingSessionRepository sessionRepository;     // CRUD ChargingSession
    private final BookingsRepository bookingsRepository;           // Đọc Booking phục vụ phiên sạc
    private final ChargingSessionMapper mapper;                    // Map Entity <-> DTO response
    private final NotificationsRepository notificationsRepository; // Lưu Notification
    private final SessionSocCache sessionSocCache;                 // Cache tạm SOC theo session
    private final TaskScheduler taskScheduler;                     // Schedule auto-stop khi hết khung giờ
    private final StaffsRepository staffsRepository;               // Lấy staffId từ userId

    // Handler giao dịch riêng (TX độc lập) cho stop/auto-stop để cô lập rollback
    private final ChargingSessionTxHandler txHandler;
    private final ApplicationEventPublisher eventPublisher;        // Publish event (ví dụ thông báo email)

    // Múi giờ tenant (VN) dùng thống nhất
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional // Bắt đầu phiên sạc cần đảm bảo tính toàn vẹn (tạo session, đổi trạng thái booking, create noti, schedule...)
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        // 1) Tìm Booking và kiểm tra phải đang ở trạng thái CONFIRMED (đã xác nhận)
        Booking booking = bookingsRepository
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ErrorException("Booking not found or not confirmed"));

        // 2) Không cho tạo phiên mới nếu booking đã có session rồi (tránh trùng)
        sessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // 3) Lấy thời điểm hiện tại theo VN
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // 4) Tính cửa sổ thời gian hợp lệ để sạc (ưu tiên scheduledStart/End, fallback theo slot template)
        LocalDateTime windowStart = resolveWindowStart(booking);
        LocalDateTime windowEnd   = resolveWindowEnd(booking);

        // 5) Ràng buộc: chỉ được start trong [windowStart, windowEnd]
        if (now.isBefore(windowStart)) {
            throw new ErrorException("Chưa đến giờ đặt. Chỉ được bắt đầu từ: " + windowStart);
        }
        if (now.isAfter(windowEnd)) {
            throw new ErrorException("Đã quá giờ đặt (đến: " + windowEnd + "). Không thể bắt đầu.");
        }

        // 6) Giả lập SOC ban đầu (ví dụ 5-25%) để demo (thực tế lấy từ thiết bị sạc/xe)
        int initialSoc = ThreadLocalRandom.current().nextInt(5, 25);

        // 7) Tạo bản ghi ChargingSession và lưu
        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus(ChargingSessionStatus.IN_PROGRESS); // phiên đang sạc
        session.setInitialSoc(initialSoc);
        sessionRepository.save(session);

        // 8) Cache SOC để cập nhật dần trong quá trình sạc (nếu có worker cập nhật)
        sessionSocCache.put(session.getSessionId(), initialSoc);

        // 9) Đổi trạng thái booking về BOOKED (đang trong phiên sạc)
        booking.setStatus(BookingStatus.BOOKED);
        bookingsRepository.save(booking);

        // 🔟 Lên lịch auto-stop đúng thời điểm windowEnd (theo VN) để đảm bảo không vượt khung
        Instant triggerInstant = windowEnd.atZone(TENANT_ZONE).toInstant();
        Date triggerAt = Date.from(triggerInstant);
        Long sid = session.getSessionId();

        log.info("[SCHEDULE STOP] sessionId={} bookingId={} triggerAt(VN)={} now(VN)={}",
                sid, booking.getBookingId(), triggerAt, Date.from(now.atZone(TENANT_ZONE).toInstant()));

        // Đặt job: đến triggerAt thì gọi TX handler để auto-stop nếu vẫn còn IN_PROGRESS
        taskScheduler.schedule(() -> {
            try {
                txHandler.autoStopIfStillRunningTx(sid, windowEnd); // chạy trong TX riêng
            } catch (Exception ex) {
                // Không để job chết im lặng
                log.error("[SCHEDULE STOP] Uncaught error for sessionId={} windowEnd(VN)={}: {}",
                        sid, windowEnd, ex.getMessage(), ex);
            }
        }, triggerAt);

        // 1️⃣1) Tạo Notification cho user + publish event (để email/push)
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

        // 1️⃣2) Trả về DTO kết quả cho client
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
    @Transactional // Dừng phiên sạc theo yêu cầu (từ hệ thống/thiết bị…), cập nhật trạng thái & số liệu trong TX
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {
        // 1) Tìm session cần dừng
        ChargingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Chốt thời điểm kết thúc theo VN
        LocalDateTime endTime = LocalDateTime.now(TENANT_ZONE);

        // 3) Lấy SOC cuối cùng từ cache nếu có update trong lúc sạc (khác initial mới coi là hợp lệ)
        Integer cachedSoc = sessionSocCache.get(session.getSessionId()).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        // 4) Ủy quyền xử lý dừng session cho TX handler (để gom các cập nhật vào 1 TX riêng)
        return txHandler.stopSessionInternalTx(session.getSessionId(), finalSocIfAny, endTime, StopInitiator.SYSTEM_AUTO);
    }

    @Override
    @Transactional // Tài xế (chủ xe) chủ động dừng phiên sạc của chính mình
    public StopCharSessionResponse driverStopSession(Long sessionId, Long requesterUserId) {
        // 1) Tìm session và join fetch owner để kiểm tra quyền sở hữu
        ChargingSession session = sessionRepository.findWithOwnerById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Lấy userId chủ sở hữu xe của phiên sạc này
        Long ownerUserId = session.getBooking()
                .getVehicle()
                .getDriver()
                .getUser()
                .getUserId();

        // 3) Nếu requester không phải chủ sở hữu -> chặn
        if (!ownerUserId.equals(requesterUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of this session");
        }

        // 4) Lấy SOC cuối cùng từ cache nếu có
        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        // 5) Dừng session thông qua TX handler với endTime là "bây giờ" (VN)
        return txHandler.stopSessionInternalTx(sessionId, finalSocIfAny, LocalDateTime.now(TENANT_ZONE), StopInitiator.DRIVER);
    }

    @Override
    @Transactional // Tài xế (chủ xe) chủ động dừng phiên sạc của chính mình
    public StopCharSessionResponse staffStopSession(Long sessionId, Long requesterUserId) {
        // 1) Tìm session và join fetch owner để kiểm tra quyền sở hữu
        ChargingSession session = sessionRepository.findWithOwnerById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Lấy userId chủ sở hữu xe của phiên sạc này
        Long ownerUserId = session.getBooking()
                .getVehicle()
                .getDriver()
                .getUser()
                .getUserId();

        // 3) Nếu requester không phải chủ sở hữu -> chặn
        if (!ownerUserId.equals(requesterUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of this session");
        }

        // 4) Lấy SOC cuối cùng từ cache nếu có
        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        // 5) Dừng session thông qua TX handler với endTime là "bây giờ" (VN)
        return txHandler.stopSessionInternalTx(sessionId, finalSocIfAny, LocalDateTime.now(TENANT_ZONE), StopInitiator.STAFF);
    }

    @Transactional(readOnly = true) // Chỉ đọc -> tối ưu hiệu năng
    @Override
    public List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId) {
        // Lấy tất cả phiên sạc của một trạm (mới nhất trước), map sang DTO
        List<ChargingSession> sessions =
                sessionRepository.findAllByBooking_Station_StationIdOrderByStartTimeDesc(stationId);
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId) {
        // Lấy một phiên sạc theo id, không có -> báo lỗi
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ErrorException("Charging session not found"));
        return mapper.toResponse(session); // Map sang DTO để trả về
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId) {
        // Lấy các session đang hoạt động (IN_PROGRESS/ACTIVE theo repo), map kết quả
        List<ChargingSession> active = sessionRepository.findActiveSessionsByStation(stationId);
        return active.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargingSession> getAll() {
        // Trả về raw entity (dùng cho nội bộ/admin)
        return sessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ChargingSession> findById(Long sessionId) {
        // Tìm session theo id, trả Optional để caller tự xử lý
        return sessionRepository.findById(sessionId);
    }

    // ---- helpers (read-only) ----
    /**
     * Tính thời điểm bắt đầu hợp lệ:
     * - Ưu tiên Booking.scheduledStartTime nếu có.
     * - Nếu không, lấy min(startTime) theo các slot trong booking.
     * - Trả về LocalDateTime theo "timeline" VN.
     */
    private LocalDateTime resolveWindowStart(Booking booking) {
        if (booking.getScheduledStartTime() != null) return booking.getScheduledStartTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot start time"));
    }

    /**
     * Tính thời điểm kết thúc hợp lệ:
     * - Ưu tiên Booking.scheduledEndTime nếu có.
     * - Nếu không, lấy max(endTime) theo các slot trong booking.
     * - Trả về LocalDateTime theo "timeline" VN.
     */
    private LocalDateTime resolveWindowEnd(Booking booking) {
        if (booking.getScheduledEndTime() != null) return booking.getScheduledEndTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot end time"));
    }

    /**
     * Lấy danh sách phiên sạc theo pointId (điểm sạc) đã join fetch đầy đủ để map nhanh,
     * sau đó map sang DTO hiển thị.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getSessionsByPoint(Long pointId) {
        List<ChargingSession> sessions = sessionRepository.findAllByChargingPointIdDeep(pointId);
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ActiveSessionView> getActiveSessionsCompact(Long userId) {
        // 🔎 Từ userId -> staffId (tuỳ thực thể Staffs của bạn, giả sử có staff.user mapping)
        Long staffId = staffsRepository.findIdByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for current user"));

        // 👉 Query chỉ trả về session của các trạm mà staff này đang active
        return sessionRepository.findActiveSessionCompactByStaff(staffId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CompletedSessionView> getCompletedSessionsCompactByStaff(Long userId) {

        Long staffId = staffsRepository.findIdByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for current user"));

        return sessionRepository.findCompletedSessionCompactByStaff(staffId);
    }
}
