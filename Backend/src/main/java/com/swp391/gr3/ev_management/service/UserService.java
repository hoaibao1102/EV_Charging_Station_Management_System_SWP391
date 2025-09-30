package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.Users;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface UserService {
    public Users findUsersByPhone(String phoneNumber);
    public Users getUser(String phoneNumber, String password);
    public ResponseEntity<?> createUser(RegisterRequest registerRequest);
    public Users register(RegisterRequest registerRequest);
    public Users login(LoginRequest loginRequest);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);
    ResponseEntity<?> logout(HttpServletRequest request);
    Users authenticate(String phoneNumber, String password);

}
