package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class RegisterRequest {
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String name;
    private LocalDate dateOfBirth; // "2001-09-26"
    private String gender;         // có thể dùng enum
    private String address;
}


