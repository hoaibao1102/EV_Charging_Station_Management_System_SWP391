package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartCharSessionRequest {
    @NotNull(message = "Booking ID cannot be null")
    private Long bookingId;
}
