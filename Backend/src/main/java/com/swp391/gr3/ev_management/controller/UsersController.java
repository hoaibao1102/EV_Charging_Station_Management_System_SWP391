package com.swp391.gr3.ev_management.controller;

import java.util.Map;

import com.swp391.gr3.ev_management.DTO.response.LoginResponse;
import com.swp391.gr3.ev_management.service.OtpService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "APIs for user registration and authentication")
@RequiredArgsConstructor
public class UsersController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OtpService otpService;

    @PostMapping(value = "/register",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Phone Number is required");
        }
        if (userService.existsByPhoneNumber(req.getPhoneNumber())) {
            return ResponseEntity.badRequest().body("Phone Number already registered");
        }
        if (userService.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        otpService.generateOtp(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent to email " + req.getEmail()));
    }

    @PostMapping(value="/register/verify", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyOtpAndRegister(@RequestBody RegisterRequest req,
                                                  @RequestParam String otp) {
        if (!otpService.verifyOtp(req.getEmail(), otp)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message","Invalid or expired OTP"));
        }

        User created = userService.register(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message","Đăng ký thành công","data",created));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Xác thực user
            User user = userService.authenticate(loginRequest.getPhoneNumber(), loginRequest.getPassword());

            // Sinh JWT token
            String token = tokenService.generateToken(user);

            LoginResponse response = new LoginResponse(token, user.getName(), user.getPhoneNumber(), user.getEmail(), user.getGender());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @PreAuthorize("hasRole('DRIVER') or hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(HttpServletRequest request,
                                             @RequestBody CompleteProfileReq req) {
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }
        String phone = req.getPhoneNumber().trim();

        if (userService.existsByPhoneNumber(phone)) {
            return ResponseEntity.badRequest().body("Phone number already in use");
        }

        Long userId = tokenService.extractUserIdFromRequest(request);
        User u = userService.findById(userId);
        u.setPhoneNumber(phone);
        userService.addUser(u);
        return ResponseEntity.ok("Profile completed");
    }

    @Data
    public static class CompleteProfileReq {
        private String phoneNumber;
    }

}
