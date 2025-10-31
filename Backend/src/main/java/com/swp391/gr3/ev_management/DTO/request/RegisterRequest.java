package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @NotNull(message = "Email cannot be null")
    private String email;
    @NotNull(message = "Phone number cannot be null")
    private String phoneNumber;
    @NotNull(message = "Password cannot be null")
    private String passwordHash;
    @NotNull(message = "Name cannot be null")
    private String name;
    @NotNull(message = "Date of birth cannot be null")
    private LocalDate dateOfBirth; // "2001-09-26"
    @NotNull(message = "Gender cannot be null")
    private String gender;         // có thể dùng enum
    @NotNull(message = "Address cannot be null")
    private String address;
}


