package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
    @NotNull(message = "Phone number cannot be null")
    String phoneNumber;
    @NotNull(message = "Password cannot be null")
    String password;
}
