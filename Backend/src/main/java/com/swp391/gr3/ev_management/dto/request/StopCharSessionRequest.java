package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StopCharSessionRequest {
    @NotNull(message = "Session ID cannot be null")
    private Long sessionId;
}
