package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import org.springframework.stereotype.Service;

@Service
public interface StaffService {
    Staffs findByStaffId(Long staffId);
    StaffResponse updateStatus(Long userId, StaffStatus status);
}
