package com.swp391.gr3.ev_management.service;

import org.springframework.stereotype.Service;

@Service
public interface OtpService {

    String generateOtp(String email);
    boolean verifyOtp(String email, String otpCode);

}
