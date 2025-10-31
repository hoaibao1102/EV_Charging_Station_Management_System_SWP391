package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.StaffStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateStationStaffRequest {
    @NotNull(message = "Staff status cannot be null")
    private RegisterRequest user;   // thông tin user mới
    @NotNull(message = "Station ID cannot be null")
    private Long stationId;       // trạm muốn gắn
}
