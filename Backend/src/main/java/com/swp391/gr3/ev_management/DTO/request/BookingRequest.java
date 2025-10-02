package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class BookingRequest {
    private LocalDateTime bookingTime;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private String note;
}
