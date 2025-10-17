package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;          // id booking
    private String vehicleName;      // VD: VF8
    private String timeRange;        // VD: 09:00 - 10:00
    private String slotName;         // VD: Slot 01
    private String connectorType;    // VD: CCS Type 2
    private double price;
    private String stationName;      // VD: Duy Cường Station
    private LocalDateTime bookingDate;   // VD: 2025-10-15
    private BookingStatus status;
}