package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.DriverViolation;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.DriverViolationRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationServiceImpl implements ViolationService{
    private final DriverViolationRepository violationRepository;
    private final DriverRepository driverRepository;

    //CORE METHOD: Tạo violation và TỰ ĐỘNG ban nếu >= 3 vi phạm
    @Override
    @Transactional
    public ViolationResponse createViolation(Long userId, ViolationRequest request) {
        log.info("Creating violation for userId: {}", userId);

        // 1. Tìm driver theo userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        // 2. Tạo violation mới
        DriverViolation violation = DriverViolation.builder()
                .driver(driver)
                .status(ViolationStatus.ACTIVE)
                .description(request.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();

        DriverViolation savedViolation = violationRepository.save(violation);
        log.info("Violation created: ID={}, Description={}",
                savedViolation.getViolationId(), savedViolation.getDescription());

        // 3. ✅ TỰ ĐỘNG check và ban driver
        boolean wasAutoBanned = autoCheckAndBanDriver(driver);

        // 4. Build response
        return buildViolationResponse(savedViolation, wasAutoBanned);
    }

     // AUTO-BAN LOGIC: Tự động ban driver nếu có >= 3 vi phạm ACTIVE
    private boolean autoCheckAndBanDriver(Driver driver) {
        // Đếm số vi phạm ACTIVE
        int activeViolationCount = violationRepository.countByDriver_DriverIdAndStatus(
                driver.getDriverId(),
                ViolationStatus.ACTIVE
        );

        log.info("Driver {} (userId={}) now has {} ACTIVE violations",
                driver.getDriverId(), driver.getUser().getUserId(), activeViolationCount);

        // Nếu >= 3 vi phạm và chưa bị ban
        if (activeViolationCount >= 3 && driver.getStatus() != DriverStatus.BANNED) {
            log.warn("🚨 AUTO-BAN TRIGGERED: Driver {} has {} violations",
                    driver.getDriverId(), activeViolationCount);

            try {
                // 1. Set tất cả vi phạm ACTIVE → INACTIVE
                List<DriverViolation> activeViolations = violationRepository
                        .findByDriver_DriverIdAndStatus(driver.getDriverId(), ViolationStatus.ACTIVE);

                activeViolations.forEach(v -> v.setStatus(ViolationStatus.INACTIVE));
                violationRepository.saveAll(activeViolations);
                log.info("Set {} violations to INACTIVE", activeViolations.size());

                // 2. Ban driver
                driver.setStatus(DriverStatus.BANNED);
                driver.setLastActiveAt(LocalDateTime.now());
                driverRepository.save(driver);

                log.warn("✅ Driver {} (userId={}) has been AUTO-BANNED due to {} violations",
                        driver.getDriverId(), driver.getUser().getUserId(), activeViolationCount);

                // TODO: Gửi notification/email cho driver
                // notificationService.sendBanNotification(driver.getUser());

                return true;  // Đã ban

            } catch (Exception e) {
                log.error("❌ Failed to auto-ban driver {}: {}", driver.getDriverId(), e.getMessage(), e);
                throw new RuntimeException("Auto-ban failed: " + e.getMessage(), e);
            }
        }

        return false;  // Không ban
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getViolationsByUserId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        List<DriverViolation> violations = violationRepository.findByDriver_DriverId(driver.getDriverId());

        return violations.stream()
                .map(v -> buildViolationResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getViolationsByUserIdAndStatus(Long userId, ViolationStatus status) {
        List<DriverViolation> violations = violationRepository.findByUserIdAndStatus(userId, status);

        return violations.stream()
                .map(v -> buildViolationResponse(v, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveViolations(Long userId) {
        return violationRepository.countByUserIdAndStatus(userId, ViolationStatus.ACTIVE);
    }

    /**
     * Helper: Build ViolationResponse
     */
    private ViolationResponse buildViolationResponse(DriverViolation violation, boolean wasAutoBanned) {
        Driver driver = violation.getDriver();

        String message = null;
        if (wasAutoBanned) {
            message = "Driver has been AUTO-BANNED due to 3 or more violations";
        }

        return ViolationResponse.builder()
                .violationId(violation.getViolationId())
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .driverName(driver.getUser().getName())
                .status(violation.getStatus())
                .description(violation.getDescription())
                .occurredAt(violation.getOccurredAt())
                .driverAutoBanned(wasAutoBanned)
                .message(message)
                .build();
    }
}
