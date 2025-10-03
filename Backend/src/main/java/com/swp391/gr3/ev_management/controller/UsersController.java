package com.swp391.gr3.ev_management.controller;

import java.util.HashMap;
import java.util.Map;

import com.swp391.gr3.ev_management.DTO.response.LoginResponse;
import com.swp391.gr3.ev_management.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.Users;
import com.swp391.gr3.ev_management.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/users")
public class UsersController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/register",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        Users created = userService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đăng ký thành công", "data", created));
    }

    @PostMapping(value = "/createUser",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return userService.createUser(registerRequest); // service đã trả ResponseEntity
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Xác thực user
            Users user = userService.authenticate(loginRequest.getPhoneNumber(), loginRequest.getPassword());

            // Sinh JWT token
            String token = tokenService.generateToken(user);

            LoginResponse response = new LoginResponse(token, user.getFullName(), user.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return userService.logout(request);
    }
}
