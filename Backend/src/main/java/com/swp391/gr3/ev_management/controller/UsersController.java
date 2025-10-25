package com.swp391.gr3.ev_management.controller;

import java.util.Map;

import com.swp391.gr3.ev_management.DTO.request.*;
import com.swp391.gr3.ev_management.DTO.response.LoginResponse;
import com.swp391.gr3.ev_management.service.AuthService;
import com.swp391.gr3.ev_management.service.OtpService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
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

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "APIs for user registration and authentication")
@RequiredArgsConstructor
public class UsersController {
    private final UserService userService;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final AuthService authService;

    @PostMapping(value = "/register",
            consumes = "application/json",
            produces = "application/json")
    @Operation(summary = "Request OTP for registration",
               description = "Sends an OTP to the user's email for verification during registration")
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
    @Operation(summary = "Verify OTP and complete registration",
               description = "Verifies the OTP sent to the user's email and completes the registration process")
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
    @Operation(summary = "User login",
               description = "Authenticates user and returns JWT token upon successful login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Xác thực user
            User user = userService.authenticate(loginRequest.getPhoneNumber(), loginRequest.getPassword());

            // Sinh JWT token
            String token = tokenService.generateToken(user);

            LoginResponse response = new LoginResponse(token, user.getName(), user.getPhoneNumber(), user.getEmail(), user.getGender(), user.getRole().getRoleName());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout",
               description = "Logs out the user by invalidating the JWT token")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @PreAuthorize("hasRole('DRIVER') or hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/complete-profile")
    @Operation(summary = "Complete user profile",
               description = "Allows users to complete their profile by adding missing information such as phone number")
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

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP", description = "Gửi OTP qua email để đặt lại mật khẩu")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.sendResetOtp(req.getEmail());
        // Trả message chung chung để không lộ thông tin người dùng
        return ResponseEntity.ok().body("Nếu email tồn tại, OTP đã được gửi.");
    }

//    @PostMapping("/verify-otp")
//    @Operation(summary = "Verify OTP", description = "Xác thực mã OTP đã gửi qua email")
//    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
//        boolean ok = authService.verifyResetOtp(req.getEmail(), req.getOtp());
//        return ok ? ResponseEntity.ok("OTP hợp lệ.")
//                : ResponseEntity.badRequest().body("OTP không hợp lệ hoặc đã hết hạn.");
//    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Đặt lại mật khẩu bằng OTP còn hạn")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }

}
