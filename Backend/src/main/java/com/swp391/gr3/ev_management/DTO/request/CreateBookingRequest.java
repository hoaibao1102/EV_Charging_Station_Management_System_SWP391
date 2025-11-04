package com.swp391.gr3.ev_management.DTO.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Data
public class CreateBookingRequest {
    private Long vehicleId;
    private List<Long> slotIds;
    private LocalDateTime bookingTime;
}
