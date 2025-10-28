package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.VehicleResponse;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DriverService {
    // CRUD
    DriverResponse createDriverProfile(Long idDriver, @Valid DriverRequest request);
    DriverResponse getByUserId(Long userId);
    DriverResponse getByDriverId(Long driverId);
    List<DriverResponse> getAllDrivers();
    DriverResponse updateDriverProfile(Long userId, @Valid DriverUpdateRequest updateRequest);
    DriverResponse updateStatus(Long userId, DriverStatus newStatus);
    // Filter
    List<DriverResponse> getDriversByStatus(DriverStatus status);
    List<DriverResponse> getDriversByName(String name);
    List<DriverResponse> getDriversByPhoneNumber(String phoneNumber);
    
    // UC-04: Vehicle Management
    VehicleResponse addVehicle(Long userId, @Valid AddVehicleRequest request);
    List<VehicleResponse> getMyVehicles(Long userId);
    void removeVehicle(Long userId, Long vehicleId);

    DriverResponse updateDriverPassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword);
}

