package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;            // ✅
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationServiceImpl implements ViolationService {

    private final DriverViolationRepository violationRepository;
    private final DriverRepository driverRepository;
    private final NotificationsRepository notificationsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingsRepository bookingsRepository;
    private final ChargingSessionRepository chargingSessionRepository;
    private final DriverViolationTripletRepository driverViolationTripletRepository;

    // NEW: để tính tiền phạt theo phút dựa vào Tariff
    private final TariffRepository tariffRepository;
    private final UserVehicleRepository userVehicleRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ViolationResponse createViolation(Long userId, ViolationRequest request) {
        final Long bookingId = request.getBookingId();
        log.info("Creating violation for userId={}, bookingId={}", userId, bookingId);

        if (bookingId == null) {
            log.warn("[createViolation] Skip: bookingId is null (userId={})", userId);
            return null;
        }

        // 1) Driver
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2) Booking (JOIN đủ chain connectorType qua repo findByIdWithConnectorType)
        var booking = bookingsRepository.findByIdWithConnectorType(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found with id " + bookingId));

        // 3) Rule NO-SHOW:
        //    - Phạt toàn bộ thời lượng slot nếu không có charging session hợp lệ
        //    - Chỉ xử lý khi đã qua slotEnd
        final LocalDateTime slotStart = booking.getScheduledStartTime();
        final LocalDateTime slotEnd   = booking.getScheduledEndTime();
        final LocalDateTime now       = LocalDateTime.now();

        // Chưa tới giờ bắt đầu -> bỏ qua
        if (!now.isAfter(slotStart)) {
            log.debug("[createViolation][NO_SHOW] Skip: before slot start (bookingId={}, slotStart={}, now={})",
                    bookingId, slotStart, now);
            return null;
        }

        // Có session hợp lệ -> không phải no-show (tùy rule của bạn nếu muốn đổi)
        boolean hasValidSession = chargingSessionRepository.findByBooking_BookingId(bookingId)
                .filter(cs -> cs.getStatus() == ChargingSessionStatus.COMPLETED
                        || cs.getStatus() == ChargingSessionStatus.PENDING
                        || cs.getStatus() == ChargingSessionStatus.IN_PROGRESS)
                .isPresent();
        if (hasValidSession) {
            log.debug("[createViolation][NO_SHOW] Skip: has valid charging session (bookingId={})", bookingId);
            return null;
        }

        // Chưa kết thúc slot -> đợi tới khi qua slotEnd mới kết luận no-show
        if (!now.isAfter(slotEnd)) {
            log.debug("[createViolation][NO_SHOW] Skip: slot not finished yet (bookingId={}, slotEnd={}, now={})",
                    bookingId, slotEnd, now);
            return null;
        }

        // Tính toàn bộ thời lượng giữ chỗ (ceil theo phút, tối thiểu 1)
        long reservedSeconds = Math.max(0, java.time.Duration.between(slotStart, slotEnd).getSeconds());
        if (reservedSeconds <= 0) {
            log.warn("[createViolation][NO_SHOW] Skip: invalid slot duration (bookingId={}, slotStart={}, slotEnd={})",
                    bookingId, slotStart, slotEnd);
            return null;
        }
        long penaltyMinutes = Math.max(1, (reservedSeconds + 59) / 60); // CEIL

        // 4) connectorTypeId: ưu tiên Slot -> ChargingPoint -> ConnectorType; fallback vehicle.model.connectorType
        Long connectorTypeId = null;
        try {
            connectorTypeId = booking.getBookingSlots().stream()
                    .filter(bs -> bs.getSlot() != null
                            && bs.getSlot().getChargingPoint() != null
                            && bs.getSlot().getChargingPoint().getConnectorType() != null)
                    .findFirst()
                    .map(bs -> bs.getSlot()
                            .getChargingPoint()
                            .getConnectorType()
                            .getConnectorTypeId())
                    .orElse(null);
        } catch (Exception ignored) {
            // fallback bên dưới
        }

        if (connectorTypeId == null) {
            try {
                connectorTypeId = booking.getVehicle()
                        .getModel()
                        .getConnectorType()
                        .getConnectorTypeId();
                log.warn("[createViolation][NO_SHOW] Fallback connectorTypeId via vehicle.model for bookingId={}: {}",
                        bookingId, connectorTypeId);
            } catch (Exception ex) {
                throw new ErrorException("Không xác định được ConnectorType cho bookingId " + bookingId);
            }
        }

        // 5) Tariff & tiền phạt theo toàn bộ thời lượng slot
        Tariff activeTariff = resolveActiveTariff(connectorTypeId);
        double pricePerMin = activeTariff.getPricePerMin();
        double penaltyAmount = pricePerMin * penaltyMinutes;

        log.info("[createViolation][NO_SHOW] bookingId={}, slotStart={}, slotEnd={}, minutes={}, connectorTypeId={}, pricePerMin={}, penalty={}",
                bookingId, slotStart, slotEnd, penaltyMinutes, connectorTypeId, pricePerMin, penaltyAmount);

        // 6) Lưu violation (flush ngay) — occurredAt đặt tại thời điểm kết thúc slot để phản ánh no-show
        DriverViolation savedViolation = violationRepository.saveAndFlush(
                DriverViolation.builder()
                        .driver(driver)
                        .status(ViolationStatus.ACTIVE)
                        .description(
                                Optional.ofNullable(request.getDescription())
                                        .orElse("No-show: reserved slot not used"))
                        .occurredAt(slotEnd)
                        .penaltyAmount(penaltyAmount)
                        .build()
        );
        log.info("[createViolation][NO_SHOW] Saved violationId={} for bookingId={}", savedViolation.getViolationId(), bookingId);

        // 👇 Gọi ở đây
        attachViolationToTriplet(driver, savedViolation);

        // 7) Auto-ban nếu cần
        boolean wasAutoBanned = autoCheckAndBanDriver(driver);
        return buildViolationResponse(savedViolation, wasAutoBanned);
    }

    // Lấy connectorTypeId từ vehicle của driver (VehicleModel.connectorType)
    private Long resolveConnectorTypeIdFromDriverVehicles(Long driverId) {
        var vehicles = userVehicleRepository.findByDriverIdWithModelAndConnector(driverId);
        if (vehicles == null || vehicles.isEmpty()) {
            throw new ErrorException("Driver has no vehicle with connector type configured");
        }
        return vehicles.get(0).getModel().getConnectorType().getConnectorTypeId();
    }

    // Lấy Tariff đang hiệu lực cho connectorTypeId (now nằm trong khoảng hiệu lực)
    private Tariff resolveActiveTariff(Long connectorTypeId) {
        var now = LocalDateTime.now();
        return tariffRepository.findActiveByConnectorType(connectorTypeId, now)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ErrorException(
                        "No active tariff found for connectorTypeId " + connectorTypeId));
    }

    // ✅ TỰ ĐỘNG BAN + gửi NOTIFICATION (email sẽ do listener Thymeleaf lo)
    private boolean autoCheckAndBanDriver(Driver driver) {
        int activeViolationCount = violationRepository.countByDriver_DriverIdAndStatus(
                driver.getDriverId(), ViolationStatus.ACTIVE);

        log.info("Driver {} (userId={}) now has {} ACTIVE violations",
                driver.getDriverId(), driver.getUser().getUserId(), activeViolationCount);

        if (activeViolationCount >= 3 && driver.getStatus() != DriverStatus.BANNED) {
            log.warn("AUTO-BAN TRIGGERED: Driver {} has {} violations", driver.getDriverId(), activeViolationCount);

            // 1) đóng tất cả vi phạm ACTIVE -> INACTIVE
            List<DriverViolation> activeViolations =
                    violationRepository.findByDriver_DriverIdAndStatus(driver.getDriverId(), ViolationStatus.ACTIVE);
            activeViolations.forEach(v -> v.setStatus(ViolationStatus.INACTIVE));
            violationRepository.saveAll(activeViolations);

            // 2) BAN driver
            driver.setStatus(DriverStatus.BANNED);
            driver.setLastActiveAt(LocalDateTime.now());
            driverRepository.save(driver);

            // 3) Tạo NOTIFICATION cho user (KHÔNG tham chiếu booking)
            Notification noti = new Notification();
            noti.setUser(driver.getUser());
            noti.setTitle("Tài khoản bị khóa do vi phạm");
            noti.setContentNoti("Tài khoản của bạn đã bị khóa tự động vì có từ 3 vi phạm trở lên. "
                    + "Vui lòng liên hệ hỗ trợ hoặc tới trạm gần nhất để được xưử lý theo chính sách của chúng tôi.");
            noti.setType(NotificationTypes.USER_BANNED); // ⚠️ enum phải đúng chính tả
            noti.setStatus("UNREAD");
            noti.setCreatedAt(LocalDateTime.now());
            notificationsRepository.save(noti);

            // 4) Publish event -> NotificationEmailListener sẽ gửi mail Thymeleaf
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getViolationsByUserId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        return violationRepository.findByDriver_DriverId(driver.getDriverId())
                .stream().map(v -> buildViolationResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getViolationsByUserIdAndStatus(Long userId, ViolationStatus status) {
        return violationRepository.findByUserIdAndStatus(userId, status)
                .stream().map(v -> buildViolationResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveViolations(Long userId) {
        return violationRepository.countByUserIdAndStatus(userId, ViolationStatus.ACTIVE);
    }

    private ViolationResponse buildViolationResponse(DriverViolation violation, boolean wasAutoBanned) {
        Driver driver = violation.getDriver();
        return ViolationResponse.builder()
                .violationId(violation.getViolationId())
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .driverName(driver.getUser().getName())
                .status(violation.getStatus())
                .description(violation.getDescription())
                .occurredAt(violation.getOccurredAt())
                // Giữ nguyên cấu trúc cũ của response
                .driverAutoBanned(wasAutoBanned)
                .message(wasAutoBanned ? "Driver has been AUTO-BANNED due to 3 or more violations" : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    // 👇 Private helper nằm trong cùng class, chạy trong cùng @Transactional ở trên
    public void attachViolationToTriplet(Driver driver, DriverViolation violation) {
        // không cho 1 violation nằm ở 2 nhóm
        if (driverViolationTripletRepository.existsByViolation(violation.getViolationId())) return;

        // lấy hoặc tạo nhóm OPEN
        DriverViolationTriplet triplet = driverViolationTripletRepository.findOpenByDriver(driver.getDriverId())
                .stream().findFirst().orElse(null);

        if (triplet == null || triplet.getCountInGroup() >= 3) {
            triplet = DriverViolationTriplet.builder()
                    .driver(driver)
                    .status(TripletStatus.OPEN)
                    .countInGroup(0)
                    .totalPenalty(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            triplet = driverViolationTripletRepository.save(triplet);
        }

        // cập nhật nhóm
        if (triplet.getCountInGroup() == 0) {
            triplet.setV1(violation);
            triplet.setWindowStartAt(violation.getOccurredAt());
        } else if (triplet.getCountInGroup() == 1) {
            triplet.setV2(violation);
        } else {
            triplet.setV3(violation);
        }
        triplet.setCountInGroup(triplet.getCountInGroup() + 1);
        triplet.setTotalPenalty(triplet.getTotalPenalty() + violation.getPenaltyAmount());

        if (triplet.getCountInGroup() == 3) {
            triplet.setStatus(TripletStatus.CLOSED);
            triplet.setWindowEndAt(violation.getOccurredAt());
            triplet.setClosedAt(LocalDateTime.now());
        }

        driverViolationTripletRepository.save(triplet);
    }
}
