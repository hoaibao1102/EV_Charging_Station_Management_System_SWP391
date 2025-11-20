package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopSessionForStaffRequest {

    @NotNull(message = "Session ID cannot be null")
    @Positive(message = "Session ID must be positive")
    private Long sessionId;

    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    private Long userId;
}
