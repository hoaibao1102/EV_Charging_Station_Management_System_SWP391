package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.dto.request.DriverRequest;
import com.swp391.gr3.ev_management.dto.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateVehicleRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingSessionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

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

    DriverResponse updateDriverPassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword);

    List<TransactionBriefResponse> getMyTransactions(Long userId);
    List<ChargingSessionBriefResponse> getMyChargingSessions(Long userId);
}

