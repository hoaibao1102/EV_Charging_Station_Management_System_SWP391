package com.swp391.gr3.ev_management.service;


import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.emuns.DriverStatus;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Upgrade a User to Driver.
     * Creates Driver entity and domino affect by create user
     *
     * @throws NotFoundException if the user doesn't exist
     * @throws ConflictException if the user is already a driver
     */
    @Override
    @Transactional
    public DriverResponse createDriverProfile(Long userId, DriverRequest request) {
        log.info("Creating driver for user {}", userId);

        User user = userRepository.findUserByUserId(userId);
        if (user == null)
            throw new NotFoundException("User not found with ID: " + userId);

        if (driverRepository.existsByDriverId(userId))
            throw new ConflictException("Driver already exists for userId " + userId);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setStatus(request.getDriverStatus() != null ? request.getDriverStatus() : DriverStatus.ACTIVE);

        Driver saved = driverRepository.save(driver);
        return mapToDriverResponse(saved);
    }

    @Override
    public DriverResponse getByUserId(Long driverId) {
        Driver driver = driverRepository.findByIdWithUser(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with ID " + driverId));
        return mapToDriverResponse(driver);
    }

    @Override
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    @Override
    @Transactional
    public DriverResponse updateDriverProfile(Long userId, DriverUpdateRequest req) {
        Driver driver = driverRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        User user = driver.getUser();
        if (req.getName() != null) user.setName(req.getName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());

        if (req.getDriverStatus() != null) driver.setStatus(req.getDriverStatus());

        Driver updated = driverRepository.save(driver);
        return mapToDriverResponse(updated);
    }
    @Override
    @Transactional
    public DriverResponse updateStatus(Long userId, DriverStatus status) {
        Driver driver = driverRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));
        driver.setStatus(status);
        driverRepository.save(driver);
        return mapToDriverResponse(driver);
    }


    /**
     * Get driver profile by ID
     */
    @Transactional(readOnly = true)
    public DriverResponse getDriverProfile(Long driverId) {
        Driver driver = driverRepository.findByIdWithUser(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + driverId));

        return mapToDriverResponse(driver);
    }

    @Override
    public List<DriverResponse> getDriversByStatus(DriverStatus status) {
        return driverRepository.findByStatus(status)
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    @Override
    public List<DriverResponse> getDriversByName(String name) {
        return driverRepository.findByUser_NameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    @Override
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
