package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    /**
     * Tạo driver profile cho 1 user (nâng cấp thành Driver)
     */
    @Override
    @Transactional
    public DriverResponse createDriverProfile(Long userId, DriverRequest request) {
        log.info("Creating driver for user {}", userId);

        User user = userRepository.findUserByUserId(userId);
        if (user == null) {
            throw new NotFoundException("User not found with ID: " + userId);
        }

        // Check trùng đúng cách theo userId
        if (driverRepository.existsByUser_UserId(userId)) {
            throw new ConflictException("Driver already exists for userId " + userId);
        }

        Driver driver = new Driver();
        driver.setUser(user);
        DriverStatus status = (request != null && request.getDriverStatus() != null)
                ? request.getDriverStatus()
                : DriverStatus.ACTIVE;
        driver.setStatus(status);

        try {
            Driver saved = driverRepository.save(driver);
            return mapToDriverResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Phòng race condition: nếu unique(UserID) ở DB bắn lỗi
            throw new ConflictException("Driver already exists for userId " + userId);
        }
    }

    /**
     * Lấy driver theo userId (dùng cho màn self-profile qua token)
     */
    @Override
    @Transactional(readOnly = true)
    public DriverResponse getByUserId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));
        return mapToDriverResponse(driver);
    }

    /**
     * Lấy driver theo driverId (dùng khi admin thao tác theo PK driver)
     */
    @Override
    @Transactional(readOnly = true)
    public DriverResponse getByDriverId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with driverId " + userId));
        return mapToDriverResponse(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    /**
     * Driver tự cập nhật hồ sơ — userId lấy từ token
     */
    @Override
    @Transactional
    public DriverResponse updateDriverProfile(Long userId, DriverUpdateRequest req) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        User user = driver.getUser();
        if (req.getName() != null)        user.setName(req.getName());
        if (req.getEmail() != null)       user.setEmail(req.getEmail());
        if (req.getAddress() != null)     user.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        driver.setStatus(DriverStatus.ACTIVE);

        Driver updated = driverRepository.save(driver);
        return mapToDriverResponse(updated);
    }

    /**
     * Admin đổi trạng thái driver theo userId (đúng với flow controller của bạn)
     */
    @Override
    @Transactional
    public DriverResponse updateStatus(Long userId, DriverStatus status) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));
        driver.setStatus(status);
        driverRepository.save(driver);
        return mapToDriverResponse(driver);
    }

    /**
     * Lấy driver profile theo driverId (nếu bạn cần riêng)
     */
    @Transactional(readOnly = true)
    public DriverResponse getDriverProfile(Long driverId) {
        Driver driver = driverRepository.findByDriverIdWithUser(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with driverId: " + driverId));
        return mapToDriverResponse(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByStatus(DriverStatus status) {
        return driverRepository.findByStatus(status)
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByName(String name) {
        return driverRepository.findByUser_NameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByPhoneNumber(String phoneNumber) {
        return driverRepository.findByUser_PhoneNumberContaining(phoneNumber)
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    private DriverResponse mapToDriverResponse(Driver driver) {
        return DriverResponse.builder()
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .email(driver.getUser().getEmail())
                .phoneNumber(driver.getUser().getPhoneNumber())
                .name(driver.getUser().getName())
                .status(driver.getStatus())
                .address(driver.getUser().getAddress())
                .createdAt(driver.getUser().getCreatedAt())
                .updatedAt(driver.getUser().getUpdatedAt())
                .build();
    }
}