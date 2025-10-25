package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;
}
