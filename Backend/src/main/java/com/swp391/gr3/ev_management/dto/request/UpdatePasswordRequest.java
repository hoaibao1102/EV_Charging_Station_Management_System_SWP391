package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotNull(message = "Old password cannot be null")
    private String oldPassword;
    @NotNull(message = "New password cannot be null")
    private String newPassword;
    @Size(min = 6, message = "newPassword must be at least 6 characters")
    @NotNull(message = "Confirm password cannot be null")
    private String confirmPassword;
}
