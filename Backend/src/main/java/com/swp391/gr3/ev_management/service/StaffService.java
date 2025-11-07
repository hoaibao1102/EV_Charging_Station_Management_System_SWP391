package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.dto.response.StaffResponse;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StaffService {
    StaffResponse updateStatus(Long userId, StaffStatus status);
    StaffResponse updateProfile(Long userId, UpdateStaffProfileRequest request);
    void updatePassword(Long userId, UpdatePasswordRequest request);
    List<StaffResponse> getAll();
}
