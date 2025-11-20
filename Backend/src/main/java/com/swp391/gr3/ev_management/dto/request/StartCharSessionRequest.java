package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StartCharSessionRequest {

    @NotNull(message = "Booking ID cannot be null")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;
}
