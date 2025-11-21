package com.swp391.gr3.ev_management.service;

public interface AuthService {

    void sendResetOtp(String email);
    
    void resetPassword(String email, String otp, String newPassword);
}
