package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;            // ✅
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        // 2) Booking (JOIN FETCH đủ chain connector + vehicle.model.connectorType để fallback)
        var booking = bookingsRepository.findByIdWithConnectorType(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id " + bookingId));

        // 3) Tính quá hạn: FLOOR theo phút (+ optional GRACE)
        final int GRACE_SECONDS = 0; // đổi 60 nếu muốn miễn phạt 60s đầu
        LocalDateTime slotEnd = booking.getScheduledEndTime();
        LocalDateTime now = LocalDateTime.now();

        long overdueSec = Math.max(0, java.time.Duration.between(slotEnd, now).getSeconds());
        if (overdueSec <= GRACE_SECONDS) {
            log.debug("[createViolation] Skip within grace/not overdue (bookingId={}, slotEnd={}, now={}, overdueSec={}, grace={})",
                    bookingId, slotEnd, now, overdueSec, GRACE_SECONDS);
            return null;
        }
        long overdueMinutes = (overdueSec - GRACE_SECONDS) / 60; // FLOOR
        if (overdueMinutes <= 0) {
            log.debug("[createViolation] Skip: not enough whole minutes (bookingId={}, overdueSec={}, grace={})",
                    bookingId, overdueSec, GRACE_SECONDS);
            return null;
        }

        // 4) ConnectorTypeId: ưu tiên qua BookingSlot -> Slot -> ChargingPoint -> ConnectorType
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
        } catch (Exception ignored) {}

        // Fallback: vehicle.model.connectorType
        if (connectorTypeId == null) {
            try {
                connectorTypeId = booking.getVehicle()
                        .getModel()
                        .getConnectorType()
                        .getConnectorTypeId();
                log.warn("[createViolation] Fallback connectorTypeId via vehicle.model for bookingId={}: {}",
                        bookingId, connectorTypeId);
            } catch (Exception ex) {
                throw new NotFoundException("Không xác định được ConnectorType cho bookingId " + bookingId);
            }
        }

        // 5) Tariff active & tiền phạt
        Tariff activeTariff = resolveActiveTariff(connectorTypeId);
        double pricePerMin = activeTariff.getPricePerMin();
        double penaltyAmount = pricePerMin * overdueMinutes;

        log.info("[createViolation] Calc(FLOOR): bookingId={}, slotEnd={}, now={}, overdueSec={}, grace={}, overdueMin={}, connectorTypeId={}, pricePerMin={}, penalty={}",
                bookingId, slotEnd, now, overdueSec, GRACE_SECONDS, overdueMinutes, connectorTypeId, pricePerMin, penaltyAmount);

        // 6) Lưu violation (flush ngay) — kèm liên kết booking nếu schema có cột BookingID
        DriverViolation.DriverViolationBuilder builder = DriverViolation.builder()
                .driver(driver)
                .status(ViolationStatus.ACTIVE)
                .description(request.getDescription())
                .occurredAt(now)
                .penaltyAmount(penaltyAmount);
        try {
        } catch (Throwable ignore) {}

        DriverViolation savedViolation = violationRepository.saveAndFlush(builder.build());
        log.info("[createViolation] Saved violationId={} for bookingId={}", savedViolation.getViolationId(), bookingId);

        // 7) Auto-ban
        boolean wasAutoBanned = autoCheckAndBanDriver(driver);
        return buildViolationResponse(savedViolation, wasAutoBanned);
    }

    // Lấy connectorTypeId từ vehicle của driver (VehicleModel.connectorType)
    private Long resolveConnectorTypeIdFromDriverVehicles(Long driverId) {
        var vehicles = userVehicleRepository.findByDriverIdWithModelAndConnector(driverId);
        if (vehicles == null || vehicles.isEmpty()) {
            throw new NotFoundException("Driver has no vehicle with connector type configured");
        }
        return vehicles.get(0).getModel().getConnectorType().getConnectorTypeId();
    }

    // Lấy Tariff đang hiệu lực cho connectorTypeId (now nằm trong khoảng hiệu lực)
    private Tariff resolveActiveTariff(Long connectorTypeId) {
        var now = LocalDateTime.now();
        return tariffRepository.findActiveByConnectorType(connectorTypeId, now)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
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
                    + "Vui lòng liên hệ hỗ trợ để được xem xét mở khóa.");
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
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

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


}
