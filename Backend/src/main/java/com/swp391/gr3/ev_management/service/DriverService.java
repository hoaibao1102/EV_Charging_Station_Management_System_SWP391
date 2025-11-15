package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.*;
import com.swp391.gr3.ev_management.dto.response.ChargingSessionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface DriverService {
    // CRUD
    DriverResponse createDriverProfile(Long idDriver, @Valid DriverRequest request);
    DriverResponse getByUserId(Long userId);
    List<DriverResponse> getAllDrivers();
    DriverResponse updateDriverProfile(Long userId, @Valid DriverUpdateRequest updateRequest);
    DriverResponse updateStatus(Long userId, DriverStatus newStatus);
    
    // UC-04: Vehicle Management
    VehicleResponse addVehicle(Long userId, @Valid AddVehicleRequest request);
    List<VehicleResponse> getMyVehicles(Long userId);

    VehicleResponse updateVehicle(Long userId, Long vehicleId, UpdateVehicleRequest request);
    VehicleResponse updateVehicleStatus(Long userId, Long vehicleId, UserVehicleStatus status);

    DriverResponse updateDriverPassword(Long userId, UpdatePasswordRequest request);

    List<TransactionBriefResponse> getMyTransactions(Long userId);
    List<ChargingSessionBriefResponse> getMyChargingSessions(Long userId);

    Optional<Driver> findByUser_UserId(Long userId);

    long count();

    long countByStatus(DriverStatus driverStatus);

    Optional<Driver> findByUserIdWithUser(Long userId);

    void save(Driver driver);
}

