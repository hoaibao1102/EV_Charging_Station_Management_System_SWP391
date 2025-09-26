package com.swp391.gr3.ev_management.controller;

import java.util.Map;

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

    @PostMapping("login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            userService.login(req);
            Users u = userService.findUsersByPhone(req.getPhoneNumber());
            return ResponseEntity.ok(
                    java.util.Map.of("message", "Đăng nhập thành công", "data", u)
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body(
                    java.util.Map.of("message", "Sai số điện thoại hoặc mật khẩu")
            );
        }
    }
}
