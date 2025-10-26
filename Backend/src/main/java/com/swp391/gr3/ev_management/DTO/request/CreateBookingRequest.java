package com.swp391.gr3.ev_management.DTO.request;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {
  private Long vehicleId;
  private List<Long> slotIds;
  private LocalDateTime bookingTime;
}
