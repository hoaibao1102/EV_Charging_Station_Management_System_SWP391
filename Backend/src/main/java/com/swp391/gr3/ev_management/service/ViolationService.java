package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.ViolationResponse;
import com.swp391.gr3.ev_management.enums.ViolationStatus;

import java.util.List;

public interface ViolationService {

    //Tạo violation mới và TỰ ĐỘNG ban nếu >= 3 vi phạm
    ViolationResponse createViolation(Long userId, ViolationRequest request);

    //Lấy tất cả vi phạm của driver theo userId
    List<ViolationResponse> getViolationsByUserId(Long userId);

    //Lấy vi phạm theo status
    List<ViolationResponse> getViolationsByUserIdAndStatus(Long userId, ViolationStatus status);

    //Đếm số vi phạm ACTIVE của driver
    int countActiveViolations(Long userId);

//    ViolationResponse createViolation(Long userId, Long bookingId, String description);
}
