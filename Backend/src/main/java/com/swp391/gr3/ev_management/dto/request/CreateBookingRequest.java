package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingRequest {

//    @NotNull(message = "ID xe không được để trống")
    @Positive(message = "ID xe phải là số dương")
    private Long vehicleId;

    @NotEmpty(message = "Danh sách slot không được để trống")
    private List<Long> slotIds;

    private LocalDateTime bookingTime;
}
