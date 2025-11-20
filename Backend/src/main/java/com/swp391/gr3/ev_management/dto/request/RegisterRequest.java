package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message="Invalid VN phone")
    private String phoneNumber;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String passwordHash; // thực tế nên đặt là 'password' hơn là 'passwordHash'

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender cannot be blank")
    private String gender; // có thể chuyển sang enum

    @NotBlank(message = "Address cannot be blank")
    private String address;
}


