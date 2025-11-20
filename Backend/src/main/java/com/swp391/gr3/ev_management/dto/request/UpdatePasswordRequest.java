package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "Old password cannot be null")
    @Size(min = 6, message = "oldPassword must be at least 6 characters")
    private String oldPassword;

    @NotBlank(message = "New password cannot be null")
    @Size(min = 6, message = "newPassword must be at least 6 characters")
    private String newPassword;

    @Size(min = 6, message = "newPassword must be at least 6 characters")
    @NotBlank(message = "Confirm password cannot be null")
    private String confirmPassword;
}
