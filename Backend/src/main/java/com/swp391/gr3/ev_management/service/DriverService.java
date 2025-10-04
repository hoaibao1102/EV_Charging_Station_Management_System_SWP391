package com.swp391.gr3.ev_management.service;


import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.DriverWallet;
import com.swp391.gr3.ev_management.entity.Users;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.DriverWalletRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverWalletRepository walletRepository;

    /**
     * Upgrade a User to Driver.
     * Creates Driver entity and auto-creates DriverWallet with balance = 0.
     * 
     * @throws NotFoundException if user doesn't exist
     * @throws ConflictException if user is already a driver
     */
    @Transactional
    public DriverResponse upgradeToDriver(Long userId, DriverRequest request) {
        log.info("Attempting to upgrade user {} to driver", userId);

        // 1. Check user exists
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException("User not found with ID: " + userId));

        // 2. Check if already a driver
        if (driverRepository.existsByUser_UserId(userId)) {
            throw new ConflictException("User is already a driver with ID: " + userId);
        }

        // 3. Create Driver entity (driverId will be set to userId via @MapsId)
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setDriverId(userId); // Explicitly set to match userId
        driver.setStatus(request.getStatus() != null ? request.getStatus() : "Pending");

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver created with ID: {}", savedDriver.getDriverId());

        // 4. Create Driver_Wallet with balance = 0
        DriverWallet wallet = new DriverWallet();
        wallet.setDriver(savedDriver);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");

        walletRepository.save(wallet);
        log.info("Wallet created for driver {}", savedDriver.getDriverId());

        return mapToDriverResponse(savedDriver);
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

    private DriverResponse mapToDriverResponse(Driver driver) {
        return DriverResponse.builder()
                .driverId(driver.getDriverId())
                .userId(driver.getUser().getUserId())
                .email(driver.getUser().getEmail())
                .phoneNumber(driver.getUser().getPhoneNumber())
                .firstName(driver.getUser().getFirstName())
                .lastName(driver.getUser().getLastName())
                .status(String.valueOf(driver.getDriverStatus()))
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
