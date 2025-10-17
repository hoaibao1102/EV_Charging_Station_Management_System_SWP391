package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.StationStaffResponse;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
public interface StaffStationService {
    StationStaffResponse getStaffByUserId(Long userId);
}
