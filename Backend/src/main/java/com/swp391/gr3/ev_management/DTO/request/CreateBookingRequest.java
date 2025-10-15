package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {
  private Long vehicleId;
  private Long slotId;
  private Long connectorTypeId;
  private LocalDateTime bookingTime;



}
