package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID must not be null")
    @Positive(message = "Vehicle ID must be positive")
    private Long vehicleId;

    @NotEmpty(message = "Slot IDs must not be empty")
    private List<Long> slotIds;

    private LocalDateTime bookingTime;
}
