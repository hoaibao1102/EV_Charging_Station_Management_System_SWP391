package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public User findUsersByPhone(String phoneNumber) {
        return userRepository.findUsersByPhoneNumber(phoneNumber);
    }

    public User findUsersById(Long userId) {
        return userRepository.findUserByUserId(userId);
    }

    @Override
    public User getUser(String phoneNumber, String password) {
        return userRepository.findUsersByPhoneNumberAndPasswordHash(phoneNumber, password);
    }

    @Transactional
    @Override
    public User register(RegisterRequest r) {
        if (r.getPasswordHash() == null || r.getPasswordHash().isBlank())
            throw new IllegalArgumentException("Password is required");
        if (userRepository.existsByPhoneNumber(r.getPhoneNumber()))
            throw new IllegalStateException("Phone number already in use");
        if (r.getEmail() != null && userRepository.existsByEmail(r.getEmail()))
            throw new IllegalStateException("Email already in use");

        Role role = roleRepository.findByRoleId(3L); // Default role is USER with roleId = 3
        if (role == null) throw new IllegalStateException("Role not found");

        User u = new User();
        u.setEmail(r.getEmail());
        u.setPhoneNumber(r.getPhoneNumber());
        u.setPasswordHash(passwordEncoder.encode(r.getPasswordHash()));
        u.setDateOfBirth(r.getDateOfBirth());
        u.setGender(r.getGender());
        u.setAddress(r.getAddress());
        u.setRole(role);

        return userRepository.save(u);
    }

    @Override
    public ResponseEntity<?> createUser(RegisterRequest r) {
        Role role = roleRepository.findByRoleId(3L); // Default role is USER with roleId = 3
        if (role == null) throw new IllegalStateException("Default role not found");

        User u = new User();
        u.setEmail(r.getEmail());
        u.setPhoneNumber(r.getPhoneNumber());
        u.setPasswordHash(passwordEncoder.encode(r.getPasswordHash()));
        u.setName(r.getName());
        u.setDateOfBirth(r.getDateOfBirth());
        u.setGender(r.getGender());
        u.setAddress(r.getAddress());
        u.setRole(role);
        userRepository.save(u);

        return ResponseEntity.ok("User created successfully");
    }

    public User login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhoneNumber(),
                        loginRequest.getPassword()
                )
        );

        // Get the username (phone number) from the principal
        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // Fetch your Users entity from the database
        return userRepository.findUsersByPhoneNumber(springUser.getUsername());
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        return userRepository.existsByPhoneNumber(phoneNumber.trim());
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("No Bearer token found");
        }

        String token = authHeader.substring(7).trim();
        // Lấy thời điểm expire từ token
        Instant expiry;
        try {
            expiry = tokenService.getExpirationFromJwt(token); // trả về Instant
        } catch (Exception ex) {
            // Nếu token không hợp lệ, vẫn xóa cookie/client side nhưng server-side không cần lưu
            return ResponseEntity.badRequest().body("Invalid token");
        }

        long ttlSeconds = expiry.getEpochSecond() - Instant.now().getEpochSecond();
        if (ttlSeconds <= 0) {
            // token đã hết hạn => không cần blacklist
            return ResponseEntity.ok("Token already expired");
        }

        // nếu bạn đang gửi token qua cookie, trả cookie xoá ở đây (tùy app)
        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public User authenticate(String phoneNumber, String rawPassword) {
        // Tìm user theo số điện thoại
        User user = userRepository.findUsersByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new IllegalArgumentException("Số điện thoại không tồn tại");
        }

        // So sánh mật khẩu nhập vào với mật khẩu đã mã hoá trong DB
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không chính xác");
        }

        return user; // trả về user nếu đăng nhập thành công
    }

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }


}
