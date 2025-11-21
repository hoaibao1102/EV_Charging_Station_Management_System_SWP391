package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserVehicleService {

    long countByModel_ModelId(Long id);

    Optional<UserVehicle> findById(Long vehicleId);

    Optional<Long> findConnectorTypeIdByVehicleId(Long vehicleId);
}
