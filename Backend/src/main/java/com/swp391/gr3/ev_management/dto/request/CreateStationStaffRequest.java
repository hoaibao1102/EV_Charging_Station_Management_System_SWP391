package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CreateStationStaffRequest {
    @NotNull(message = "Staff status cannot be null")
    private RegisterRequest user;   // thông tin user mới
    @NotNull(message = "Station ID cannot be null")
    private Long stationId;       // trạm muốn gắn
}
