package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.StationStaff;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface StationStaffService {

    Optional<StationStaff> findActiveByStationStaffId(@NotNull(message = "Staff ID cannot be null") Long staffId);

    Optional<StationStaff> findActiveByUserId(Long userId);

    Optional<StationStaff> findActiveByStaffId(Long staffId);

    void save(StationStaff active);

    StationStaff saveStationStaff(StationStaff stationStaff);
}
