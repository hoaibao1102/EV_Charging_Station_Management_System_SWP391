package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "Booking ID cannot be null")
    private Long bookingId;
    @NotNull(message = "Station ID cannot be null")
    private Long stationId;
    @NotNull(message = "Vehicle ID cannot be null")
    private Long vehicleId;
    @NotNull(message = "Booking time cannot be null")
    private LocalDateTime bookingTime;
    @NotNull(message = "Scheduled start time cannot be null")
    private LocalDateTime scheduledStartTime;
    @NotNull(message = "Scheduled end time cannot be null")
    private LocalDateTime scheduledEndTime;
    @NotNull(message = "Status cannot be null")
    private String status;
}
