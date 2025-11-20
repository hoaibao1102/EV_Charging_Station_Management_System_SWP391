package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "Booking ID cannot be null")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;

    @NotNull(message = "Station ID cannot be null")
    @Positive(message = "Station ID must be positive")
    private Long stationId;

    @NotNull(message = "Vehicle ID cannot be null")
    @Positive(message = "Vehicle ID must be positive")
    private Long vehicleId;

    @NotNull(message = "Booking time cannot be null")
    private LocalDateTime bookingTime;

    @NotNull(message = "Scheduled start time cannot be null")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "Scheduled end time cannot be null")
    private LocalDateTime scheduledEndTime;

    @NotBlank(message = "Status cannot be blank")
    private String status;
}
