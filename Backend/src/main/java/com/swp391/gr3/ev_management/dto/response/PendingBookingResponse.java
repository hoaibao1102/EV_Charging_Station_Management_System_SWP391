package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PendingBookingResponse {

    private Long bookingId;
    private String customerName;
    private String vehiclePlate;
    private String stationName;
    private LocalDateTime startTime;
    private BookingStatus status;   // hoặc String nếu bạn muốn, Jackson sẽ serialize thành "PENDING"
}
