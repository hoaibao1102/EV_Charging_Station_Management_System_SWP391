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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationServiceImpl implements ViolationService {

    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DriverViolationRepository violationRepository;
    private final DriverRepository driverRepository;
    private final NotificationsRepository notificationsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingsRepository bookingsRepository;
    private final ChargingSessionRepository chargingSessionRepository;
    private final DriverViolationTripletRepository driverViolationTripletRepository;
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

        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        Booking booking = bookingsRepository.findByIdWithConnectorType(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found with id " + bookingId));

        final LocalDateTime slotStart = booking.getScheduledStartTime();
        final LocalDateTime slotEnd   = booking.getScheduledEndTime();
        final LocalDateTime now       = LocalDateTime.now(TENANT_ZONE);

        // Trước giờ bắt đầu -> bỏ qua (only now < start)
        if (now.isBefore(slotStart)) {
            log.debug("[NO_SHOW] Skip: before slot start (bookingId={}, slotStart={}, now={})", bookingId, slotStart, now);
            return null;
        }

        boolean hasValidSession = chargingSessionRepository.findByBooking_BookingId(bookingId)
                .filter(cs -> cs.getStatus() == ChargingSessionStatus.COMPLETED
                        || cs.getStatus() == ChargingSessionStatus.PENDING
                        || cs.getStatus() == ChargingSessionStatus.IN_PROGRESS)
                .isPresent();
        if (hasValidSession) {
            log.debug("[NO_SHOW] Skip: has valid charging session (bookingId={})", bookingId);
            return null;
        }

        // Chưa kết thúc slot -> đợi (only now < end)
        if (now.isBefore(slotEnd)) {
            log.debug("[NO_SHOW] Skip: slot not finished yet (bookingId={}, slotEnd={}, now={})", bookingId, slotEnd, now);
            return null;
        }
        // Đến đây: now >= slotEnd  => tạo violation

        long reservedSeconds = Math.max(0, java.time.Duration.between(slotStart, slotEnd).getSeconds());
        if (reservedSeconds <= 0) {
            log.warn("[NO_SHOW] Skip: invalid slot duration (bookingId={}, slotStart={}, slotEnd={})",
                    bookingId, slotStart, slotEnd);
            return null;
        }
        long penaltyMinutes = Math.max(1, (reservedSeconds + 59) / 60);

        Long connectorTypeId = null;
        try {
            connectorTypeId = booking.getBookingSlots().stream()
                    .filter(bs -> bs.getSlot() != null
                            && bs.getSlot().getChargingPoint() != null
                            && bs.getSlot().getChargingPoint().getConnectorType() != null)
                    .findFirst()
                    .map(bs -> bs.getSlot().getChargingPoint().getConnectorType().getConnectorTypeId())
                    .orElse(null);
        } catch (Exception ignored) {}

        if (connectorTypeId == null) {
            try {
                connectorTypeId = booking.getVehicle().getModel().getConnectorType().getConnectorTypeId();
                log.warn("[NO_SHOW] Fallback connectorTypeId via vehicle.model for bookingId={}: {}",
                        bookingId, connectorTypeId);
            } catch (Exception ex) {
                throw new ErrorException("Không xác định được ConnectorType cho bookingId " + bookingId);
            }
        }

        Tariff activeTariff = resolveActiveTariff(connectorTypeId);
        double pricePerMin = Optional.ofNullable(activeTariff.getPricePerMin()).orElse(0.0);
        double penaltyAmount = pricePerMin * penaltyMinutes;

        log.info("[NO_SHOW] bookingId={}, slotStart={}, slotEnd={}, minutes={}, connectorTypeId={}, pricePerMin={}, penalty={}",
                bookingId, slotStart, slotEnd, penaltyMinutes, connectorTypeId, pricePerMin, penaltyAmount);

        DriverViolation savedViolation = violationRepository.saveAndFlush(
                DriverViolation.builder()
                        .driver(driver)
                        .status(ViolationStatus.ACTIVE)
                        .description(Optional.ofNullable(request.getDescription()).orElse("No-show: reserved slot not used"))
                        .occurredAt(slotEnd)
                        .penaltyAmount(penaltyAmount)
                        .build()
        );
        log.info("[NO_SHOW] Saved violationId={} for bookingId={}", savedViolation.getViolationId(), bookingId);

        attachViolationToTriplet(driver, savedViolation);

        boolean wasAutoBanned = autoCheckAndBanDriver(driver);
        return buildViolationResponse(savedViolation, wasAutoBanned);
    }

    private Tariff resolveActiveTariff(Long connectorTypeId) {
        var now = LocalDateTime.now(TENANT_ZONE);
        return tariffRepository.findActiveByConnectorType(connectorTypeId, now)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[NO_SHOW] No active tariff for connectorTypeId={}, fallback pricePerMin=0", connectorTypeId);
                    Tariff t = new Tariff();
                    t.setPricePerMin(0.0);
                    return t;
                });
    }

    private boolean autoCheckAndBanDriver(Driver driver) {
        int activeViolationCount = violationRepository.countByDriver_DriverIdAndStatus(
                driver.getDriverId(), ViolationStatus.ACTIVE);

        log.info("Driver {} (userId={}) now has {} ACTIVE violations",
                driver.getDriverId(), driver.getUser().getUserId(), activeViolationCount);

        if (activeViolationCount >= 3 && driver.getStatus() != DriverStatus.BANNED) {
            log.warn("AUTO-BAN TRIGGERED: Driver {} has {} violations", driver.getDriverId(), activeViolationCount);

            List<DriverViolation> activeViolations =
                    violationRepository.findByDriver_DriverIdAndStatus(driver.getDriverId(), ViolationStatus.ACTIVE);
            activeViolations.forEach(v -> v.setStatus(ViolationStatus.INACTIVE));
            violationRepository.saveAll(activeViolations);

            driver.setStatus(DriverStatus.BANNED);
            driver.setLastActiveAt(LocalDateTime.now(TENANT_ZONE));
            driverRepository.save(driver);

            Notification noti = new Notification();
            noti.setUser(driver.getUser());
            noti.setTitle("Tài khoản bị khóa do vi phạm");
            noti.setContentNoti("Tài khoản của bạn đã bị khóa tự động vì có từ 3 vi phạm trở lên. "
                    + "Vui lòng liên hệ hỗ trợ hoặc tới trạm gần nhất để được xử lý.");
            noti.setType(NotificationTypes.USER_BANNED);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setCreatedAt(LocalDateTime.now(TENANT_ZONE));
            notificationsRepository.save(noti);

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
                .driverAutoBanned(wasAutoBanned)
                .message(wasAutoBanned ? "Driver has been AUTO-BANNED due to 3 or more violations" : null)
                .build();
    }

    @Override
    @Transactional // KHÔNG readOnly vì có ghi
    public void attachViolationToTriplet(Driver driver, DriverViolation violation) {
        if (driverViolationTripletRepository.existsByViolation(violation.getViolationId())) return;

        DriverViolationTriplet triplet = driverViolationTripletRepository.findOpenByDriver(driver.getDriverId())
                .stream().findFirst().orElse(null);

        if (triplet == null || triplet.getCountInGroup() >= 3) {
            triplet = DriverViolationTriplet.builder()
                    .driver(driver)
                    .status(TripletStatus.IN_PROGRESS)
                    .countInGroup(0)
                    .totalPenalty(0)
                    .createdAt(LocalDateTime.now(TENANT_ZONE))
                    .build();
            triplet = driverViolationTripletRepository.save(triplet);
        }

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
            triplet.setStatus(TripletStatus.OPEN);
            triplet.setWindowEndAt(violation.getOccurredAt());
            triplet.setClosedAt(LocalDateTime.now(TENANT_ZONE));
        }

        driverViolationTripletRepository.save(triplet);
    }
}