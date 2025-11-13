package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface OtpService {

    String generateOtp(String email);
    boolean verifyOtp(String email, String otpCode);

    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    void save(OtpVerification latest);
}
