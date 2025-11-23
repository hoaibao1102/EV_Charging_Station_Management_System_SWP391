package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.LightBookingInfo;
import com.swp391.gr3.ev_management.dto.request.ViolationRequest;
import com.swp391.gr3.ev_management.dto.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ViolationResponseMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;            // ✅ Publish event khi có Noti
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Đánh dấu đây là 1 Spring Service chứa nghiệp vụ xử lý Violation
@RequiredArgsConstructor // Lombok tạo constructor cho các field final (DI)
@Slf4j // Tự động tạo logger (log.info, log.warn, log.error, ...)
public class ViolationServiceImpl implements ViolationService {

    // Múi giờ dùng chung cho hệ thống (Việt Nam)
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ====== Dependencies được inject ======
    private final DriverViolationRepository violationRepository;                // CRUD cho DriverViolation (bản ghi vi phạm)
    private final DriverService driverService;                                  // Dùng để lấy Driver (tài xế)
    private final NotificationsService notificationsService;                    // Lưu Notification
    private final ApplicationEventPublisher eventPublisher;                     // Bắn event khi tạo noti
    private final BookingService bookingService;                                // Đọc Booking liên quan
    private final ChargingSessionService chargingSessionService;                // Kiểm tra phiên sạc có tồn tại không
    private final DriverViolationTripletService driverViolationTripletService;  // Gom nhóm 3 lỗi (Triplet)
    private final TariffService tariffService;                                  // Lấy tariff để tính tiền phạt
    private final ViolationResponseMapper violationResponseMapper;              // Map Entity -> ViolationResponse
    private final SlotAvailabilityService slotAvailabilityService;              // Lấy thông tin slot
    private final UserVehicleService userVehicleService;                        // Lấy thông tin vehicle

    @Override
    public ViolationResponse createViolation(Long userId, ViolationRequest request) {

        Long bookingId = request.getBookingId();
        log.info("[NO_SHOW] Create violation userId={}, bookingId={}", userId, bookingId);

        if (bookingId == null) return null;

    /* =======================================================
       1) Load thông tin booking — KHÔNG JOIN NHIỀU BẢNG
       ======================================================= */
        LightBookingInfo bookingInfo = bookingService.findLightBookingInfo(bookingId)
                .orElse(null);

        if (bookingInfo == null) {
            log.warn("[NO_SHOW] Booking not found {}", bookingId);
            return null;
        }

        LocalDateTime slotStart = bookingInfo.getStart();
        LocalDateTime slotEnd = bookingInfo.getEnd();
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        if (now.isBefore(slotStart)) return null;
        if (now.isBefore(slotEnd)) return null;

    /* =======================================================
       2) Check session hợp lệ - chỉ SELECT status
       ======================================================= */
        Boolean hasValidSession =
                chargingSessionService.existsValidSessionForBooking(bookingId);

        if (Boolean.TRUE.equals(hasValidSession)) {
            log.info("[NO_SHOW] Skip: Valid session exists bookingId={}", bookingId);
            return null;
        }

    /* =======================================================
       3) Load connectorTypeId – không join nặng
       ======================================================= */
        List<Long> connectorIds =
                slotAvailabilityService.findConnectorTypeIdByBooking(bookingId);

        Long connectorTypeId = connectorIds.stream()
                .findFirst()   // Optional<Long>
                .orElseGet(() ->
                        userVehicleService
                                .findConnectorTypeIdByVehicleId(bookingInfo.getVehicleId())
                                .orElse(null)
                );

        if (connectorTypeId == null) {
            throw new ErrorException("Cannot determine ConnectorType for booking " + bookingId);
        }

    /* =======================================================
       4) Load giá — query rất
       ======================================================= */
        Double pricePerMin =
                tariffService.findPricePerMinActive(connectorTypeId, now)
                        .orElse(0.0);

    /* =======================================================
       5) Tính penalty
       ======================================================= */
        long reservedSeconds = Duration.between(slotStart, slotEnd).getSeconds();
        long minutes = Math.max(1, (reservedSeconds + 59) / 60);

        double penaltyAmount = pricePerMin * minutes;

    /* =======================================================
       6) Load Driver
       ======================================================= */
        Driver driver = driverService.findByUserIdLight(userId)
                .orElseThrow(() -> new ErrorException("Driver not found"));

    /* =======================================================
       7) Save Violation
       ======================================================= */
        DriverViolation violation = DriverViolation.builder()
                .driver(driver)
                .status(ViolationStatus.ACTIVE)
                .occurredAt(slotEnd)
                .description(
                        Optional.ofNullable(request.getDescription())
                                .orElse("No-show: reserved slot not used")
                )
                .penaltyAmount(penaltyAmount)
                .build();

        violationRepository.saveAndFlush(violation);

        log.info("[NO_SHOW] Saved violationId={} bookingId={}",
                violation.getViolationId(), bookingId);

    /* =======================================================
       8) Triplet + autoban (giữ nguyên logic hiện tại)
       ======================================================= */
        attachViolationToTriplet(driver, violation);
        boolean wasAutoBanned = autoCheckAndBanDriver(driver);

        return violationResponseMapper.toResponse(violation, wasAutoBanned);
    }


    /**
     * Helper: Lấy tariff đang active cho một connectorType tại thời điểm hiện tại.
     * - Gọi TariffService.findActiveByConnectorType()
     * - Nếu không tìm thấy -> trả về tariff tạm với pricePerMin = 0 (không đánh sập flow)
     */
    private Tariff resolveActiveTariff(Long connectorTypeId) {
        var now = LocalDateTime.now(TENANT_ZONE);
        return tariffService.findActiveByConnectorType(connectorTypeId, now)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[NO_SHOW] No active tariff for connectorTypeId={}, fallback pricePerMin=0", connectorTypeId);
                    Tariff t = new Tariff();
                    t.setPricePerMin(0.0);
                    return t;
                });
    }

    /**
     * Tự động kiểm tra và BAN driver nếu có >= 3 violation ACTIVE.
     * - Nếu đủ điều kiện BAN:
     *   + set tất cả violation ACTIVE -> INACTIVE (đưa vào lịch sử)
     *   + driver.status = BANNED
     *   + gửi Notification cho user
     * - Trả về true nếu vừa BAN, ngược lại false.
     */
    private boolean autoCheckAndBanDriver(Driver driver) {
        // 1) Đếm số violation ACTIVE của driver
        int activeViolationCount = violationRepository.countByDriver_DriverIdAndStatus(
                driver.getDriverId(), ViolationStatus.ACTIVE);

        log.info("Driver {} (userId={}) now has {} ACTIVE violations",
                driver.getDriverId(), driver.getUser().getUserId(), activeViolationCount);

        // 2) Nếu >= 3 && hiện tại chưa bị BANNED -> kích hoạt auto-ban
        if (activeViolationCount >= 3 && driver.getStatus() != DriverStatus.BANNED) {
            log.warn("AUTO-BAN TRIGGERED: Driver {} has {} violations", driver.getDriverId(), activeViolationCount);

            // 3) Lấy tất cả violation ACTIVE -> set lại trạng thái INACTIVE
            List<DriverViolation> activeViolations =
                    violationRepository.findByDriver_DriverIdAndStatus(driver.getDriverId(), ViolationStatus.ACTIVE);
            activeViolations.forEach(v -> v.setStatus(ViolationStatus.INACTIVE));
            violationRepository.saveAll(activeViolations);

            // 4) Cập nhật status của driver thành BANNED, đồng thời lưu lại lastActiveAt
            driver.setStatus(DriverStatus.BANNED);
            driver.setLastActiveAt(LocalDateTime.now(TENANT_ZONE));
            driverService.save(driver);

            // 5) Tạo Notification thông báo cho user là tài khoản đã bị khoá do vi phạm
            Notification noti = new Notification();
            noti.setUser(driver.getUser());
            noti.setTitle("Tài khoản bị khóa do vi phạm");
            noti.setContentNoti("Tài khoản của bạn đã bị khóa tự động vì có từ 3 vi phạm trở lên. "
                    + "Vui lòng liên hệ hỗ trợ hoặc tới trạm gần nhất để được xử lý.");
            noti.setType(NotificationTypes.USER_BANNED);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
            notificationsService.save(noti);

            // 6) Publish event để hệ thống khác (WebSocket, email, push...) có thể xử lý real-time
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
            return true;
        }
        // Không đủ điều kiện BAN
        return false;
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc
    public List<ViolationResponse> getViolationsByUserId(Long userId) {
        // 1) Tìm driver theo userId
        Driver driver = driverService.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2) Lấy tất cả violation theo driverId
        // 3) Map từng violation sang DTO, với tham số wasAutoBanned=false (chỉ dùng khi tạo mới)
        return violationRepository.findByDriver_DriverId(driver.getDriverId())
                .stream().map(v -> violationResponseMapper.toResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc
    public List<ViolationResponse> getViolationsByUserIdAndStatus(Long userId, ViolationStatus status) {
        // 1) Lấy các violation của userId theo status truyền vào (ACTIVE / INACTIVE / ...)
        // 2) Map sang DTO, wasAutoBanned=false
        return violationRepository.findByUserIdAndStatus(userId, status)
                .stream().map(v -> violationResponseMapper.toResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc
    public int countActiveViolations(Long userId) {
        // Đếm số violation ACTIVE theo userId (dùng cho UI/logic khác)
        return violationRepository.countByUserIdAndStatus(userId, ViolationStatus.ACTIVE);
    }

    /**
     * Gắn một violation vào Triplet (group 3 vi phạm):
     * - Mỗi driver có thể có nhiều Triplet, nhưng mỗi Triplet tối đa 3 violation.
     * - Nếu có Triplet đang IN_PROGRESS (chưa đủ 3) thì thêm vào Triplet đó.
     * - Nếu không có Triplet mở hoặc Triplet đã đủ 3 -> tạo Triplet mới.
     * - Khi countInGroup == 3 -> set status = OPEN, windowEndAt & closedAt.
     */
    @Override
    @Transactional // Có thao tác ghi nên không readOnly
    public void attachViolationToTriplet(Driver driver, DriverViolation violation) {
        // 1) Nếu violation này đã được gắn vào một Triplet rồi -> bỏ qua (tránh trùng)
        if (driverViolationTripletService.existsByViolation(violation.getViolationId())) return;

        // 2) Tìm Triplet đang mở cho driver (status != CLOSED, tuỳ implementation trong repository)
        DriverViolationTriplet triplet = driverViolationTripletService.findOpenByDriver(driver.getDriverId())
                .stream().findFirst().orElse(null);

        // 3) Nếu chưa có Triplet mở hoặc Triplet hiện tại đã đủ 3 violation -> tạo Triplet mới
        if (triplet == null || triplet.getCountInGroup() >= 3) {
            triplet = DriverViolationTriplet.builder()
                    .driver(driver)
                    .status(TripletStatus.IN_PROGRESS)           // đang gom lỗi
                    .countInGroup(0)                             // chưa có violation nào
                    .totalPenalty(0)                             // tổng tiền phạt ban đầu = 0
                    .createdAt(LocalDateTime.now(TENANT_ZONE))   // thời điểm tạo
                    .build();
            triplet = driverViolationTripletService.save(triplet); // lưu Triplet mới
        }

        // 4) Gán violation vào vị trí v1/v2/v3 trong Triplet dựa trên countInGroup hiện tại
        if (triplet.getCountInGroup() == 0) {
            // Violation đầu tiên trong nhóm
            triplet.setV1(violation);
            // Mở cửa sổ thời gian (windowStartAt) bằng thời điểm violation thứ nhất
            triplet.setWindowStartAt(violation.getOccurredAt());
        } else if (triplet.getCountInGroup() == 1) {
            // Violation thứ hai
            triplet.setV2(violation);
        } else {
            // Violation thứ ba
            triplet.setV3(violation);
        }

        // 5) Tăng số lượng violation trong nhóm + cộng dồn tiền phạt
        triplet.setCountInGroup(triplet.getCountInGroup() + 1);
        triplet.setTotalPenalty(triplet.getTotalPenalty() + violation.getPenaltyAmount());

        // 6) Nếu đã đủ 3 violation trong một Triplet
        if (triplet.getCountInGroup() == 3) {
            // - Đổi trạng thái Triplet sang OPEN (đủ bộ 3, sẵn sàng để thanh toán/xử lý)
            triplet.setStatus(TripletStatus.OPEN);
            // - Đặt thời điểm kết thúc cửa sổ bằng thời điểm xảy ra violation thứ 3
            triplet.setWindowEndAt(violation.getOccurredAt());
            // - Đánh dấu thời điểm đóng Triplet (kết thúc vòng đời nhóm)
            triplet.setClosedAt(LocalDateTime.now(TENANT_ZONE));
        }

        // 7) Lưu lại Triplet (dù mới hay đã cập nhật)
        driverViolationTripletService.addDriverViolationTriplet(triplet);
    }
}
