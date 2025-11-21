package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.RegisterRequest;
import com.swp391.gr3.ev_management.dto.response.GetUsersResponse;
import com.swp391.gr3.ev_management.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UserService {

    User getUser(String phoneNumber, String password);

    User register(RegisterRequest registerRequest);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    ResponseEntity<?> logout(HttpServletRequest request);

    User authenticate(String phoneNumber, String password);

    void addUser(User user);

    List<User> findAll();

    User findById(Long id);

    User registerAsStaff(RegisterRequest req, Long stationId); // ADMIN

    Map<String, Object> registerStaffAndAssignStation(RegisterRequest user, Long stationId);

    List<GetUsersResponse> getAllUsersWithSessions();

    User findByEmail(String email);

    long count();
}
