package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateStationStaffRequest {

    @NotNull(message = "Thông tin nhân viên không được để trống")
    private RegisterRequest user;   // thông tin user mới

    @NotNull(message = "ID trạm không được để trống")
    @Positive(message = "ID trạm phải là số dương")
    private Long stationId;       // trạm muốn gắn
}
