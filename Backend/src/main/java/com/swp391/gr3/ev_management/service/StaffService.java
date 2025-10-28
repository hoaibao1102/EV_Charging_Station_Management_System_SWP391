package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import org.springframework.stereotype.Service;

@Service
public interface StaffService {
    StaffResponse updateStatus(Long userId, StaffStatus status);
    StaffResponse updateProfile(Long userId, UpdateStaffProfileRequest request);
    void updatePassword(Long userId, UpdatePasswordRequest request);
}
