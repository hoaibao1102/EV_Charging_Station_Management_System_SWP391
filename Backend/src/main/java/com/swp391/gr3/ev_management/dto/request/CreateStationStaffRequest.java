package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class CreateStationStaffRequest {

    @NotNull(message = "Staff status cannot be null")
    private RegisterRequest user;   // thông tin user mới

    @NotNull(message = "Station ID cannot be null")
    @Positive(message = "Station ID must be positive")
    private Long stationId;       // trạm muốn gắn
}
