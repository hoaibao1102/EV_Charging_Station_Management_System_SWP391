package com.swp391.gr3.ev_management.DTO.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Vehicle ID is required")
  private Long vehicleId;
    @NotNull(message = "Slot IDs are required")
  private List<Long> slotIds;
  private LocalDateTime bookingTime;
}
