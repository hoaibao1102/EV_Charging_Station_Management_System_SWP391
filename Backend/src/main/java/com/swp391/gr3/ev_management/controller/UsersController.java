package com.swp391.gr3.ev_management.controller;

import java.util.Map;

import com.swp391.gr3.ev_management.dto.request.*;
import com.swp391.gr3.ev_management.dto.response.LoginResponse;
import com.swp391.gr3.ev_management.service.AuthService;
import com.swp391.gr3.ev_management.service.OtpService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.service.UserService;

import jakarta.validation.Valid;

@RestController // ✅ Đánh dấu là REST Controller (trả JSON thay vì view)
@RequestMapping("/api/users") // ✅ Tất cả endpoint trong controller bắt đầu bằng /api/users
@Tag(name = "Users", description = "APIs for user registration and authentication")
// ✅ Swagger: nhóm các API về đăng ký, đăng nhập và xác thực người dùng
@RequiredArgsConstructor // ✅ Lombok: tạo constructor tự động cho các field final (Dependency Injection)
public class UsersController {

    // 🧩 Inject các service cần thiết để xử lý logic người dùng
    private final UserService userService;     // ✅ Xử lý thông tin người dùng (đăng ký, xác thực, lưu, cập nhật)
    private final TokenService tokenService;   // ✅ Xử lý token JWT (tạo, giải mã, xác thực)
    private final OtpService otpService;       // ✅ Quản lý OTP (gửi, kiểm tra, xác thực OTP)
    private final AuthService authService;     // ✅ Xử lý logic xác thực tổng hợp (đặt lại mật khẩu, gửi OTP,...)

    // =========================================================================
    // ✅ 1. GỬI OTP ĐỂ ĐĂNG KÝ TÀI KHOẢN MỚI
    // =========================================================================
    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Request OTP for registration",
            description = "Sends an OTP to the user's email for verification during registration"
    )
    public ResponseEntity<?> requestOtp(@Valid @RequestBody RegisterRequest req) {
        // 🟢 Kiểm tra đầu vào — email và số điện thoại phải có
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Phone Number is required");
        }

        // ❌ Kiểm tra trùng số điện thoại hoặc email
        if (userService.existsByPhoneNumber(req.getPhoneNumber())) {
            return ResponseEntity.badRequest().body("Phone Number already registered");
        }
        if (userService.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        // 🟢 Gửi OTP đến email của người dùng
        otpService.generateOtp(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent to email " + req.getEmail()));
    }

    // =========================================================================
    // ✅ 2. XÁC THỰC OTP VÀ HOÀN TẤT ĐĂNG KÝ
    // =========================================================================
    @PostMapping(value="/register/verify", produces=MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Verify OTP and complete registration",
            description = "Verifies the OTP sent to the user's email and completes the registration process"
    )
    public ResponseEntity<?> verifyOtpAndRegister(
            @RequestBody RegisterRequest req,
            @RequestParam String otp // ✅ OTP được gửi kèm trong query param
    ) {
        // ❌ Kiểm tra OTP có hợp lệ không
        if (!otpService.verifyOtp(req.getEmail(), otp)) {
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message","Invalid or expired OTP"));
        }

        // 🟢 Nếu OTP hợp lệ -> tạo mới user
        User created = userService.register(req);

        // 🟢 Trả về HTTP 201 (Created) + thông tin user vừa tạo
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message","Đăng ký thành công","data",created));
    }

    // =========================================================================
    // ✅ 3. ĐĂNG NHẬP NGƯỜI DÙNG (LOGIN)
    // =========================================================================
    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user and returns JWT token upon successful login"
    )
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 🟢 Xác thực thông tin đăng nhập (số điện thoại + mật khẩu)
            User user = userService.authenticate(loginRequest.getPhoneNumber(), loginRequest.getPassword());

            // 🟢 Sinh JWT token cho người dùng
            String token = tokenService.generateToken(user);

            // 🟢 Tạo phản hồi trả về cho client (token + thông tin user)
            LoginResponse response = new LoginResponse(
                    token,
                    user.getName(),
                    user.getPhoneNumber(),
                    user.getEmail(),
                    user.getGender(),
                    user.getRole().getRoleName()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // ❌ Sai thông tin đăng nhập -> trả về HTTP 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // =========================================================================
    // ✅ 4. ĐĂNG XUẤT (LOGOUT)
    // =========================================================================
    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logs out the user by invalidating the JWT token"
    )
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 🟢 Gọi service để vô hiệu hóa token hiện tại (đăng xuất)
        return userService.logout(request);
    }

    // =========================================================================
    // ✅ 5. HOÀN THIỆN THÔNG TIN HỒ SƠ (THÊM SỐ ĐIỆN THOẠI)
    // =========================================================================
    @PreAuthorize("hasRole('DRIVER') or hasRole('STAFF') or hasRole('ADMIN')")
    // 🔒 Chỉ user đã đăng nhập (có vai trò cụ thể) mới được phép hoàn thiện hồ sơ
    @PostMapping("/complete-profile")
    @Operation(
            summary = "Complete user profile",
            description = "Allows users to complete their profile by adding missing information such as phone number"
    )
    public ResponseEntity<?> completeProfile(HttpServletRequest request,
                                             @RequestBody CompleteProfileReq req) {
        // ❌ Kiểm tra đầu vào hợp lệ
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }
        String phone = req.getPhoneNumber().trim();

        // ❌ Kiểm tra số điện thoại đã tồn tại chưa
        if (userService.existsByPhoneNumber(phone)) {
            return ResponseEntity.badRequest().body("Phone number already in use");
        }

        // 🟢 Lấy userId từ token -> tìm user tương ứng
        Long userId = tokenService.extractUserIdFromRequest(request);
        User u = userService.findById(userId);

        // 🟢 Cập nhật số điện thoại cho user và lưu lại
        u.setPhoneNumber(phone);
        userService.addUser(u);

        return ResponseEntity.ok("Profile completed");
    }

    // ✅ DTO nội bộ (chỉ dùng trong controller) để nhận phoneNumber từ request
    @Data
    public static class CompleteProfileReq {
        private String phoneNumber;
    }

    // =========================================================================
    // ✅ 6. GỬI OTP QUÊN MẬT KHẨU (FORGOT PASSWORD)
    // =========================================================================
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset OTP",
            description = "Gửi OTP qua email để đặt lại mật khẩu"
    )
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        // 🟢 Gọi service gửi OTP đến email của người dùng (nếu tồn tại)
        authService.sendResetOtp(req.getEmail());

        // ⚠️ Trả về message chung để không lộ thông tin user (nếu email có hoặc không)
        return ResponseEntity.ok().body("Nếu email tồn tại, OTP đã được gửi.");
    }

    // =========================================================================
    // ✅ 7. ĐẶT LẠI MẬT KHẨU (RESET PASSWORD)
    // =========================================================================
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Đặt lại mật khẩu bằng OTP còn hạn"
    )
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        // 🟢 Gọi service xác thực OTP và cập nhật mật khẩu mới
        authService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }

}
