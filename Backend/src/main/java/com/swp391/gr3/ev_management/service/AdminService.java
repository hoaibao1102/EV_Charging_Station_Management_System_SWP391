package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.UpdateAdminProfileRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdatePasswordRequest;
import org.springframework.stereotype.Service;

@Service
public interface AdminService {
    void updateProfile(Long userId, UpdateAdminProfileRequest request);
    void updatePassword(Long userId, UpdatePasswordRequest request);
}
