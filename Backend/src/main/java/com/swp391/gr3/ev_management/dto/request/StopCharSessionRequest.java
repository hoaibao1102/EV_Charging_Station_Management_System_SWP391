package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopCharSessionRequest {

    @NotNull(message = "Session ID cannot be null")
    @Positive(message = "Session ID must be positive")
    private Long sessionId;

    // Optional: Final SOC from frontend (0-100)
    @Min(value = 0, message = "Final SOC must be at least 0")
    @Max(value = 100, message = "Final SOC must be at most 100")
    private Integer finalSoc;
}
