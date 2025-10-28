package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UserService {
    public User getUser(String phoneNumber, String password);
    public User register(RegisterRequest registerRequest);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByEmail(String email);
    ResponseEntity<?> logout(HttpServletRequest request);
    User authenticate(String phoneNumber, String password);
    public void addUser(User user);
    public List<User> findAll();
    public User findById(Long id);
    public User registerAsStaff(RegisterRequest req, Long stationId); // ADMIN

    Map<String, Object> registerStaffAndAssignStation(RegisterRequest user, Long stationId);
}
