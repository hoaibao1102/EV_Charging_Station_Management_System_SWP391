package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StaffStationService {
    StationStaffResponse getStaffByUserId(Long userId);
    StationStaffResponse updateStation(Long staffId, Long stationId);
    List<StationStaffResponse> getAll();

}
