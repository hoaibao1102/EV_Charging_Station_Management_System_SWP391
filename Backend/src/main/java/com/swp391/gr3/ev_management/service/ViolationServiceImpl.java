package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.DriverViolation;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.DriverViolationRepository;
import com.swp391.gr3.ev_management.repository.NotificationsRepository; // ✅
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;            // ✅
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public ViolationResponse createViolation(Long userId, ViolationRequest request) {
        log.info("Creating violation for userId: {}", userId);

        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        DriverViolation violation = DriverViolation.builder()
                .driver(driver)
                .status(ViolationStatus.ACTIVE)
                .description(request.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();

        DriverViolation savedViolation = violationRepository.save(violation);
        log.info("Violation created: ID={}, Description={}", savedViolation.getViolationId(), savedViolation.getDescription());

        boolean wasAutoBanned = autoCheckAndBanDriver(driver); // tạo noti + mail nằm trong hàm này

        return buildViolationResponse(savedViolation, wasAutoBanned);
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
                .driverAutoBanned(wasAutoBanned)
                .message(wasAutoBanned ? "Driver has been AUTO-BANNED due to 3 or more violations" : null)
                .build();
    }
}
