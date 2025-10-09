package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface UserService {
    public User findUsersByPhone(String phoneNumber);
    public User getUser(String phoneNumber, String password);
    public ResponseEntity<?> createUser(RegisterRequest registerRequest);
    public User register(RegisterRequest registerRequest);
    public User login(LoginRequest loginRequest);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);
    ResponseEntity<?> logout(HttpServletRequest request);
    User authenticate(String phoneNumber, String password);
    public User addUser(User user);
}
