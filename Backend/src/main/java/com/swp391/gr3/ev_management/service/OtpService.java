package com.swp391.gr3.ev_management.service;

import org.springframework.stereotype.Service;

@Service
public interface OtpService {

    public String generateOtp(String email);
    public boolean verifyOtp(String email, String otpCode);

}
