package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotNull(message = "Old password cannot be null")
    private String oldPassword;
    @NotNull(message = "New password cannot be null")
    private String newPassword;
    @NotNull(message = "Confirm password cannot be null")
    private String confirmPassword;
}
