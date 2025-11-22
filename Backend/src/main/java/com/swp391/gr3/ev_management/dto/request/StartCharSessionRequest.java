package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StartCharSessionRequest {

    @NotNull(message = "ID đặt lịch không được để trống")
    @Positive(message = "ID đặt lịch phải là số dương")
    private Long bookingId;
}
