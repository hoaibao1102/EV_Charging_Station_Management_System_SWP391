package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.emuns.DriverStatus;
import jakarta.validation.Valid;

import java.util.List;

public interface DriverService {
    // CRUD
    DriverResponse createDriverProfile(Long idDriver, @Valid DriverRequest request);
    DriverResponse getByUserId(Long driverId);
    List<DriverResponse> getAllDrivers();
    DriverResponse updateDriverProfile(Long userId, @Valid DriverUpdateRequest updateRequest);
    DriverResponse updateStatus(Long userId, DriverStatus newStatus);

    // Filter
    List<DriverResponse> getDriversByStatus(DriverStatus status);
    List<DriverResponse> getDriversByName(String name);
    List<DriverResponse> getDriversByPhoneNumber(String phoneNumber);
}

