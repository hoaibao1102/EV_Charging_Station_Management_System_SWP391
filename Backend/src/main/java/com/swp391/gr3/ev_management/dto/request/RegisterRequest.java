package com.swp391.gr3.ev_management.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterRequest {
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String name;
    private LocalDateTime dateOfBirth; // "2001-09-26"
    private String gender;         // có thể dùng enum
    private String address;
}


