package com.swp391.gr3.ev_management.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.gr3.ev_management.dto.request.StartCharSessionRequest;
import com.swp391.gr3.ev_management.dto.request.StopCharSessionRequest;
import com.swp391.gr3.ev_management.dto.response.ActiveSessionView;
import com.swp391.gr3.ev_management.dto.response.CompletedSessionView;
import com.swp391.gr3.ev_management.dto.response.StartCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.dto.response.ViewCharSessionResponse;
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
import com.swp391.gr3.ev_management.repository.ChargingSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service // Đánh dấu class là Spring Service (chứa nghiệp vụ phiên sạc)
@RequiredArgsConstructor // Generate constructor cho các field final (DI)
@Slf4j // Cung cấp logger
public class ChargingSessionServiceImpl implements ChargingSessionService {

    // ====== Dependencies chính ======
    private final ChargingSessionRepository chargingSessionRepository;     // Repository thao tác CRUD với bảng ChargingSession
    private final BookingService bookingService;                           // Service xử lý Booking (dùng để đọc Booking gốc của phiên sạc)
    private final ChargingSessionMapper mapper;                            // Mapper chuyển Entity ChargingSession -> các DTO response
    private final NotificationsService notificationsService;               // Service lưu Notification xuống DB
    private final SessionSocCache sessionSocCache;                         // Cache tạm thời SOC (mức pin %) theo sessionId
    private final TaskScheduler taskScheduler;                             // Bean scheduler để đặt lịch job (auto-stop khi hết giờ)
    private final StaffService staffService;                               // Service lấy staffId từ userId (nhân viên trạm)

    // Handler giao dịch riêng (TX độc lập) cho stop/auto-stop để cô lập rollback
    private final ChargingSessionTxHandler txHandler;                      // TX handler chuyên dùng cho stop session (chạy trong transaction riêng)
    private final ApplicationEventPublisher eventPublisher;                // Publish event (ví dụ NotificationCreatedEvent để gửi email/push)

    // Múi giờ tenant (VN) dùng thống nhất
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional // Bắt đầu phiên sạc cần đảm bảo tính toàn vẹn (tạo session, đổi trạng thái booking, create noti, schedule...)
    public StartCharSessionResponse startChargingSession(StartCharSessionRequest request) {
        // 1) Tìm Booking theo bookingId trong request và đảm bảo booking đang ở trạng thái CONFIRMED
        Booking booking = bookingService
                .findByBookingIdAndStatus(request.getBookingId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ErrorException("Booking not found or not confirmed"));

        // 2) Phòng vệ: nếu booking này đã có ChargingSession rồi thì không cho tạo thêm
        chargingSessionRepository.findByBooking_BookingId(booking.getBookingId())
                .ifPresent(s -> { throw new IllegalStateException("Session already exists for this booking"); });

        // 3) Thời gian hiện tại (VN)
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // 4) Khung giờ cho phép start/end
        LocalDateTime windowStart = resolveWindowStart(booking);
        LocalDateTime windowEnd   = resolveWindowEnd(booking);

        // 5) Chỉ cho start khi now nằm trong [windowStart, windowEnd]
        if (now.isBefore(windowStart)) {
            throw new ErrorException("Chưa đến giờ đặt. Chỉ được bắt đầu từ: " + windowStart);
        }
        if (now.isAfter(windowEnd)) {
            throw new ErrorException("Đã quá giờ đặt (đến: " + windowEnd + "). Không thể bắt đầu.");
        }

        // 6) Random SOC ban đầu
        int initialSoc = ThreadLocalRandom.current().nextInt(5, 25);

        // 7) Tạo ChargingSession
        ChargingSession session = new ChargingSession();
        session.setBooking(booking);
        session.setStartTime(now);
        session.setStatus(ChargingSessionStatus.IN_PROGRESS);
        session.setInitialSoc(initialSoc);
        chargingSessionRepository.save(session);

        // 8) Cache SOC
        sessionSocCache.put(session.getSessionId(), initialSoc);

        // 9) Cập nhật trạng thái Booking
        booking.setStatus(BookingStatus.BOOKED);
        bookingService.save(booking);

        // 10) Đặt lịch auto-stop
        Instant triggerInstant = windowEnd.atZone(TENANT_ZONE).toInstant();
        Date triggerAt = Date.from(triggerInstant);
        Long sid = session.getSessionId();

        log.info("[SCHEDULE STOP] sessionId={} bookingId={} triggerAt(VN)={} now(VN)={}",
                sid, booking.getBookingId(), triggerAt, Date.from(now.atZone(TENANT_ZONE).toInstant()));

        taskScheduler.schedule(() -> {
            try {
                txHandler.autoStopIfStillRunningTx(sid, windowEnd);
            } catch (Exception ex) {
                log.error("[SCHEDULE STOP] Uncaught error for sessionId={} windowEnd(VN)={}: {}",
                        sid, windowEnd, ex.getMessage(), ex);
            }
        }, triggerAt);

        // 11) Tạo Notification cho user khi phiên sạc bắt đầu (nếu tìm được user từ vehicle)
        UserVehicle vehicle = booking.getVehicle();
        Notification noti = null;
        if (vehicle != null &&
                vehicle.getDriver() != null &&
                vehicle.getDriver().getUser() != null) {

            noti = new Notification();
            noti.setUser(vehicle.getDriver().getUser());
            noti.setBooking(booking);
            noti.setSession(session);
            noti.setTitle("Bắt đầu sạc #" + booking.getBookingId());
            noti.setContentNoti("Pin hiện tại: " + initialSoc + "%");
            noti.setType(NotificationTypes.CHARGING_STARTED);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
            notificationsService.save(noti);

            // Publish event chỉ khi có noti
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        }

        // 12) Build response trả về cho client
        String vehiclePlate = null;
        if (vehicle != null) {
            vehiclePlate = vehicle.getVehiclePlate();
        }

        // ==== GET POINT NUMBER FOR THIS BOOKING ====
        String pointNumber = "Unknown";

        var firstSlot = booking.getBookingSlots().stream()
                .findFirst()
                .orElse(null);

        if (firstSlot != null &&
                firstSlot.getSlot() != null &&
                firstSlot.getSlot().getChargingPoint() != null &&
                firstSlot.getSlot().getChargingPoint().getPointNumber() != null) {

            pointNumber = firstSlot.getSlot().getChargingPoint().getPointNumber();
        }

        return StartCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .pointNumber(pointNumber)
                .bookingId(booking.getBookingId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(vehiclePlate)        // có thể null nếu booking không gắn vehicle
                .startTime(session.getStartTime())
                .status(session.getStatus())
                .initialSoc(initialSoc)
                .build();
    }

    @Override
    @Transactional // Dừng phiên sạc theo yêu cầu (từ hệ thống/thiết bị…), cập nhật trạng thái & số liệu trong TX
    public StopCharSessionResponse stopChargingSession(StopCharSessionRequest request) {
        // 1) Tìm session cần dừng theo sessionId trong request
        ChargingSession session = chargingSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Xác định thời điểm kết thúc phiên sạc theo TENANT_ZONE (VN)
        LocalDateTime endTime = LocalDateTime.now(TENANT_ZONE);

        // 3) Xác định final SOC:
        //    - Ưu tiên: SOC từ request (nếu có)
        //    - Fallback: Lấy từ cache nếu không có từ request
        Integer finalSocIfAny;
        if (request.getFinalSoc() != null && request.getFinalSoc() >= session.getInitialSoc()) {
            finalSocIfAny = request.getFinalSoc();
        } else {
            Integer cachedSoc = sessionSocCache.get(session.getSessionId()).orElse(null);
            finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                    ? cachedSoc
                    : null; // nếu không thoả điều kiện -> để null, handler sẽ tự xử lý
        }

        // 4) Ủy quyền việc dừng session cho TX handler
        //    - stopSessionInternalTx thực hiện update đầy đủ: endTime, finalSoc, status, totalEnergy,...
        //    - StopInitiator.SYSTEM_AUTO: đánh dấu tác nhân dừng là hệ thống/tự động
        return txHandler.stopSessionInternalTx(session.getSessionId(), finalSocIfAny, endTime, StopInitiator.SYSTEM_AUTO);
    }

    @Override
    @Transactional // Tài xế (chủ xe) chủ động dừng phiên sạc của chính mình
    public StopCharSessionResponse driverStopSession(Long sessionId, Long requesterUserId, Integer finalSocFromRequest) {
        // 1) Tìm session kèm thông tin owner (join fetch) để kiểm tra quyền sở hữu
        ChargingSession session = chargingSessionRepository.findWithOwnerById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Lấy userId của chủ sở hữu xe tham gia phiên sạc này
        Long ownerUserId = session.getBooking()
                .getVehicle()
                .getDriver()
                .getUser()
                .getUserId();

        // 3) Nếu user đang yêu cầu dừng không phải là chủ xe -> ném AccessDeniedException
        if (!ownerUserId.equals(requesterUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of this session");
        }

        // 4) Xác định final SOC:
        //    - Ưu tiên: SOC từ frontend request (virtualSoc đã tính realtime)
        //    - Fallback: Lấy từ cache nếu không có từ request
        //    - Validate: SOC phải >= initialSoc
        Integer finalSocIfAny = null;
        if (finalSocFromRequest != null && finalSocFromRequest >= session.getInitialSoc()) {
            finalSocIfAny = finalSocFromRequest;
        } else {
            // Fallback to cache if request doesn't provide finalSoc
            Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
            finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                    ? cachedSoc
                    : null;
        }

        // 5) Gọi TX handler để dừng session
        //    - StopInitiator.DRIVER: đánh dấu là tài xế chủ động dừng
        //    - endTime = thời điểm hiện tại theo VN
        return txHandler.stopSessionInternalTx(sessionId, finalSocIfAny, LocalDateTime.now(TENANT_ZONE), StopInitiator.DRIVER);
    }

    @Override
    @Transactional // Tài xế (chủ xe) chủ động dừng phiên sạc của chính mình (ở đây là STAFF dừng, nhưng code đang kiểm owner)
    public StopCharSessionResponse staffStopSession(Long sessionId) {
        // 1) Tìm session kèm thông tin owner (join fetch) để kiểm tra quyền
        ChargingSession session = chargingSessionRepository.findWithOwnerById(sessionId)
                .orElseThrow(() -> new ErrorException("Session not found"));

        // 2) Lấy userId của chủ sở hữu xe
//        Long ownerUserId = session.getBooking()
//                .getVehicle()
//                .getDriver()
//                .getUser()
//                .getUserId();

        // 4) Lấy SOC cuối cùng từ cache
        Integer cachedSoc = sessionSocCache.get(sessionId).orElse(null);
        Integer finalSocIfAny = (cachedSoc != null && !cachedSoc.equals(session.getInitialSoc()))
                ? cachedSoc
                : null;

        // 5) Dừng session thông qua TX handler
        //    - StopInitiator.STAFF: đánh dấu người dừng là nhân viên
        return txHandler.stopSessionInternalTx(sessionId, finalSocIfAny, LocalDateTime.now(TENANT_ZONE), StopInitiator.STAFF);
    }

    @Transactional(readOnly = true) // Chỉ đọc -> tối ưu hiệu năng
    @Override
    public List<ViewCharSessionResponse> getAllSessionsByStation(Long stationId) {
        // Lấy tất cả phiên sạc của một trạm (stationId), sắp xếp theo startTime giảm dần (mới nhất trước)
        List<ChargingSession> sessions =
                chargingSessionRepository.findAllByBooking_Station_StationIdOrderByStartTimeDesc(stationId);
        // Map từng ChargingSession -> ViewCharSessionResponse DTO bằng mapper
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ViewCharSessionResponse getCharSessionById(Long sessionId) {
        // Lấy một phiên sạc theo sessionId
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ErrorException("Charging session not found"));
        // Map sang DTO để trả cho client
        return mapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getActiveCharSessionsByStation(Long stationId) {
        // Tìm tất cả session đang hoạt động (active/in-progress) theo stationId
        List<ChargingSession> active = chargingSessionRepository.findActiveSessionsByStation(stationId);
        // Map sang DTO view ngắn gọn
        return active.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargingSession> getAll() {
        // Lấy toàn bộ entity ChargingSession (dùng cho mục đích nội bộ/admin, không map DTO)
        return chargingSessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ChargingSession> findById(Long sessionId) {
        // Tìm session theo id, trả Optional để caller tự xử lý có/không
        return chargingSessionRepository.findById(sessionId);
    }

    // ---- helpers (read-only) ----
    /**
     * Tính thời điểm bắt đầu hợp lệ của khung giờ sạc cho một booking:
     * - Nếu Booking.scheduledStartTime != null -> ưu tiên dùng giá trị này (đã được set sẵn khi confirm booking).
     * - Nếu không có, duyệt qua các bookingSlots của booking:
     *   + Lấy slot.date kết hợp với slot.template.startTime để ra LocalDateTime thực tế.
     *   + Lấy giá trị nhỏ nhất (min) trong các startTime đó.
     * - Nếu không tìm được slot nào -> ném ErrorException.
     */
    private LocalDateTime resolveWindowStart(Booking booking) {
        if (booking.getScheduledStartTime() != null) return booking.getScheduledStartTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot start time"));
    }

    /**
     * Tính thời điểm kết thúc hợp lệ của khung giờ sạc cho một booking:
     * - Nếu Booking.scheduledEndTime != null -> dùng giá trị này.
     * - Nếu không, duyệt toàn bộ bookingSlots:
     *   + Lấy slot.date kết hợp với slot.template.endTime để ra LocalDateTime.
     *   + Lấy giá trị lớn nhất (max) trong các endTime đó.
     * - Nếu không có slot nào -> ném ErrorException.
     */
    private LocalDateTime resolveWindowEnd(Booking booking) {
        if (booking.getScheduledEndTime() != null) return booking.getScheduledEndTime();
        return booking.getBookingSlots().stream()
                .map(bs -> bs.getSlot().getDate().with(bs.getSlot().getTemplate().getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new ErrorException("Booking has no slot end time"));
    }

    /**
     * Lấy danh sách phiên sạc theo pointId (điểm sạc),
     * repository đã join fetch đủ thông tin liên quan (station, booking, vehicle, ...)
     * => mapper có thể map sang ViewCharSessionResponse một cách đầy đủ mà không bị N+1 query.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ViewCharSessionResponse> getSessionsByPoint(Long pointId) {
        List<ChargingSession> sessions = chargingSessionRepository.findAllByChargingPointIdDeep(pointId);
        return sessions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ActiveSessionView> getActiveSessionsCompact(Long userId) {
        // 1) Từ userId (user login là staff) -> tìm staffId tương ứng
        Long staffId = staffService.findIdByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for current user"));

        // 2) Repository query trả về các session đang active thuộc các trạm mà staff này phụ trách
        //    - Dùng projection ActiveSessionView để trả dữ liệu gọn hơn (chỉ các field cần thiết)
        return chargingSessionRepository.findActiveSessionCompactByStaff(staffId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CompletedSessionView> getCompletedSessionsCompactByStaff(Long userId) {

        // 1) Lấy staffId từ userId (staff hiện tại)
        Long staffId = staffService.findIdByUserId(userId)
                .orElseThrow(() -> new ErrorException("Staff not found for current user"));

        // 2) Lấy danh sách phiên sạc đã hoàn thành (completed) thuộc các trạm mà staff này phụ trách
        //    - Dùng projection CompletedSessionView cho dữ liệu gọn
        return chargingSessionRepository.findCompletedSessionCompactByStaff(staffId);
    }

    @Override
    public List<ChargingSession> findAllByDriverUserIdDeep(Long userId) {
        // Lấy toàn bộ phiên sạc theo userId của driver, join fetch sâu (deep) để tránh N+1
        return chargingSessionRepository.findAllByDriverUserIdDeep(userId);
    }

    @Override
    public double sumEnergyAll() {
        // Tính tổng toàn bộ energy (kWh) của tất cả phiên sạc (dùng cho dashboard/thống kê)
        return chargingSessionRepository.sumEnergyAll();
    }

    @Override
    public long countAll() {
        // Đếm tổng số phiên sạc
        return chargingSessionRepository.countAll();
    }

    @Override
    public long countByStationBetween(Long stationId, LocalDateTime yearFrom, LocalDateTime yearTo) {
        // Đếm số phiên sạc tại một station trong khoảng thời gian [yearFrom, yearTo]
        // (dùng cho biểu đồ theo năm/tháng, v.v.)
        return chargingSessionRepository.countByStationBetween(stationId, yearFrom, yearTo);
    }

    @Override
    public long countSessionsByUserId(Long userId) {
        // Đếm tổng số phiên sạc mà một user (driver) đã từng thực hiện
        return chargingSessionRepository.countSessionsByUserId(userId);
    }

    @Override
    public Optional<ChargingSession> findByBooking_BookingId(Long bookingId) {
        // Tìm phiên sạc theo bookingId (1-1): dùng khi cần kiểm tra booking đã có session chưa
        return chargingSessionRepository.findByBooking_BookingId(bookingId);
    }

    @Override
    public long countByStatus(ChargingSessionStatus active) {
        // Đếm số phiên sạc theo trạng thái (status)
        return chargingSessionRepository.countByStatus(active);
    }

    @Override
    public List<ChargingSession> findTop5ByOrderByStartTimeDesc() {
        // Lấy 5 phiên sạc mới nhất (theo startTime giảm dần)
        return chargingSessionRepository.findTop5ByOrderByStartTimeDesc();
    }

    @Override
    public List<ChargingSession> findByStartTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        // Lấy danh sách phiên sạc bắt đầu trong khoảng [startOfDay, endOfDay]
        return chargingSessionRepository.findByStartTimeBetween(startOfDay, endOfDay);
    }

    @Override
    public Boolean existsValidSessionForBooking(Long bookingId) {
        return chargingSessionRepository.existsValidSessionForBooking(bookingId);
    }
}
