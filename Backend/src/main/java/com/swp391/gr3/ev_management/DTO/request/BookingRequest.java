package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    private Long bookingId;
    private Long stationId;
    private Long vehicleId;
    private LocalDateTime bookingTime;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private String status;
}
