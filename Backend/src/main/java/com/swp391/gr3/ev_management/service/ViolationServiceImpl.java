package com.swp391.gr3.ev_management.service;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final DriverViolationRepository violationRepository;              // CRUD cho DriverViolation (bản ghi vi phạm)
    private final DriverService driverService;                                // Dùng để lấy Driver (tài xế)
    private final NotificationsService notificationsService;                  // Lưu Notification
    private final ApplicationEventPublisher eventPublisher;                   // Bắn event khi tạo noti
    private final BookingService bookingService;                              // Đọc Booking liên quan
    private final ChargingSessionService chargingSessionService;              // Kiểm tra phiên sạc có tồn tại không
    private final DriverViolationTripletService driverViolationTripletService;// Gom nhóm 3 lỗi (Triplet)
    private final TariffService tariffService;                                // Lấy tariff để tính tiền phạt
    private final ViolationResponseMapper violationResponseMapper;            // Map Entity -> ViolationResponse

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Tạo 1 transaction riêng biệt (dù bên ngoài có TX hay không)
    public ViolationResponse createViolation(Long userId, ViolationRequest request) {
        final Long bookingId = request.getBookingId();
        log.info("Creating violation for userId={}, bookingId={}", userId, bookingId);

        // 1) Nếu bookingId null thì không xử lý (đảm bảo tránh NullPointer)
        if (bookingId == null) {
            log.warn("[createViolation] Skip: bookingId is null (userId={})", userId);
            return null;
        }

        // 2) Tìm Driver theo userId (join cả User) -> nếu không thấy thì ném lỗi
        Driver driver = driverService.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 3) Tìm Booking theo bookingId, đã load cả ConnectorType (qua slot/point/connector) để tính phí
        Booking booking = bookingService.findByIdWithConnectorType(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found with id " + bookingId));

        // 4) Lấy thời gian bắt đầu/kết thúc slot đã đặt + thời điểm hiện tại (VN time)
        final LocalDateTime slotStart = booking.getScheduledStartTime();
        final LocalDateTime slotEnd   = booking.getScheduledEndTime();
        final LocalDateTime now       = LocalDateTime.now(TENANT_ZONE);

        // 5) Nếu hiện tại trước giờ bắt đầu slot → chưa thể coi là no-show, bỏ qua
        if (now.isBefore(slotStart)) {
            log.debug("[NO_SHOW] Skip: before slot start (bookingId={}, slotStart={}, now={})", bookingId, slotStart, now);
            return null;
        }

        // 6) Kiểm tra xem booking này đã có phiên sạc hợp lệ chưa
        //    Nếu đã có session IN_PROGRESS / PENDING / COMPLETED thì không coi là no-show
        boolean hasValidSession = chargingSessionService.findByBooking_BookingId(bookingId)
                .filter(cs -> cs.getStatus() == ChargingSessionStatus.COMPLETED
                        || cs.getStatus() == ChargingSessionStatus.PENDING
                        || cs.getStatus() == ChargingSessionStatus.IN_PROGRESS)
                .isPresent();
        if (hasValidSession) {
            log.debug("[NO_SHOW] Skip: has valid charging session (bookingId={})", bookingId);
            return null;
        }

        // 7) Nếu slot chưa kết thúc (now < slotEnd) thì chưa xử lý no-show (chờ hết khung)
        if (now.isBefore(slotEnd)) {
            log.debug("[NO_SHOW] Skip: slot not finished yet (bookingId={}, slotEnd={}, now={})", bookingId, slotEnd, now);
            return null;
        }
        // 8) Tới đây: now >= slotEnd, không có session hợp lệ => driver đã đặt nhưng không đến => no-show

        // 9) Tính số giây slot đã giữ (từ slotStart tới slotEnd), đảm bảo không âm
        long reservedSeconds = Math.max(0, java.time.Duration.between(slotStart, slotEnd).getSeconds());
        if (reservedSeconds <= 0) {
            // Nếu thời lượng slot không hợp lệ thì log cảnh báo và bỏ qua
            log.warn("[NO_SHOW] Skip: invalid slot duration (bookingId={}, slotStart={}, slotEnd={})",
                    bookingId, slotStart, slotEnd);
            return null;
        }

        // 10) Quy đổi sang phút, làm tròn lên (mỗi 60s -> 1 phút, phần dư cũng tính 1 phút)
        long penaltyMinutes = Math.max(1, (reservedSeconds + 59) / 60);

        // 11) Lấy connectorTypeId để lấy tariff theo loại đầu nối
        Long connectorTypeId = null;
        try {
            // Ưu tiên lấy từ bookingSlots -> slot -> chargingPoint -> connectorType
            connectorTypeId = booking.getBookingSlots().stream()
                    .filter(bs -> bs.getSlot() != null
                            && bs.getSlot().getChargingPoint() != null
                            && bs.getSlot().getChargingPoint().getConnectorType() != null)
                    .findFirst()
                    .map(bs -> bs.getSlot().getChargingPoint().getConnectorType().getConnectorTypeId())
                    .orElse(null);
        } catch (Exception ignored) {}

        // 12) Nếu không tìm được qua slot, fallback: lấy từ vehicle.model.connectorType
        if (connectorTypeId == null) {
            try {
                connectorTypeId = booking.getVehicle().getModel().getConnectorType().getConnectorTypeId();
                log.warn("[NO_SHOW] Fallback connectorTypeId via vehicle.model for bookingId={}: {}",
                        bookingId, connectorTypeId);
            } catch (Exception ex) {
                // Nếu vẫn không xác định được => lỗi nghiệp vụ
                throw new ErrorException("Không xác định được ConnectorType cho bookingId " + bookingId);
            }
        }

        // 13) Lấy tariff đang active cho connectorTypeId tại thời điểm hiện tại
        Tariff activeTariff = resolveActiveTariff(connectorTypeId);
        // pricePerMin có thể null -> fallback 0.0
        double pricePerMin = Optional.ofNullable(activeTariff.getPricePerMin()).orElse(0.0);
        // 14) Tính số tiền phạt = đơn giá theo phút * số phút giữ slot
        double penaltyAmount = pricePerMin * penaltyMinutes;

        log.info("[NO_SHOW] bookingId={}, slotStart={}, slotEnd={}, minutes={}, connectorTypeId={}, pricePerMin={}, penalty={}",
                bookingId, slotStart, slotEnd, penaltyMinutes, connectorTypeId, pricePerMin, penaltyAmount);

        // 15) Tạo object DriverViolation và lưu ngay xuống DB (saveAndFlush để có ID dùng tiếp)
        DriverViolation savedViolation = violationRepository.saveAndFlush(
                DriverViolation.builder()
                        .driver(driver) // gán tài xế bị phạt
                        .status(ViolationStatus.ACTIVE) // trạng thái ACTIVE
                        .description(Optional.ofNullable(request.getDescription())
                                .orElse("No-show: reserved slot not used")) // mô tả, nếu request không có thì ghi mặc định
                        .occurredAt(slotEnd) // thời điểm vi phạm: cuối slot
                        .penaltyAmount(penaltyAmount) // số tiền phạt
                        .build()
        );
        log.info("[NO_SHOW] Saved violationId={} for bookingId={}", savedViolation.getViolationId(), bookingId);

        // 16) Đưa violation này vào Triplet (nhóm tối đa 3 vi phạm) để quản lý việc đóng/mở
        attachViolationToTriplet(driver, savedViolation);

        // 17) Kiểm tra tự động: nếu số vi phạm ACTIVE >= 3 thì tự động BAN driver
        boolean wasAutoBanned = autoCheckAndBanDriver(driver);

        // 18) Trả về DTO response kèm cờ wasAutoBanned (cho client biết driver vừa bị ban hay chưa)
        return violationResponseMapper.toResponse(savedViolation, wasAutoBanned);
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
