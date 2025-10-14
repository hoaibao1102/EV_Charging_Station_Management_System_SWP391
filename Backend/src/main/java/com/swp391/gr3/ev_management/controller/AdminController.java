package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.CreateStationStaffRequest;
import com.swp391.gr3.ev_management.DTO.response.GetUsersResponse;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.TokenService;
import com.swp391.gr3.ev_management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;


    @Autowired
    public AdminController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @GetMapping("/allUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<GetUsersResponse> getAllUsers() {
        return userService.findAll()
                .stream()
                .map(user -> new GetUsersResponse(
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getName(),
                        user.getDateOfBirth(),
                        user.getGender(),
                        user.getAddress(),
                        user.getRole().getRoleName()
                ))
                .collect(Collectors.toList());
    }

    // ADMIN: đăng ký user -> gắn làm staff
    @PostMapping(value = "/register-staff", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminRegisterStaff(@Valid @RequestBody CreateStationStaffRequest req) {
        User created = userService.registerAsStaff(
                req.getUser(),
                req.getStationId(),
                req.getAssignedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đăng ký staff thành công",
                        "userId", created.getUserId()));
    }
}
