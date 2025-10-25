package com.swp391.gr3.ev_management.service;

public interface AuthService {
    void sendResetOtp(String email);
    boolean verifyResetOtp(String email, String otp);
    void resetPassword(String email, String otp, String newPassword);
}
