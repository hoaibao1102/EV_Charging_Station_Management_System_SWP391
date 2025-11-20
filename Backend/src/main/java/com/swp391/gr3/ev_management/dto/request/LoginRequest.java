package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Phone number cannot be null")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message="Invalid VN phone")
    private String phoneNumber;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
