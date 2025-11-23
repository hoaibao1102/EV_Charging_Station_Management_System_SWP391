package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopSessionForStaffRequest {

    @NotNull(message = "ID phiên sạc không được để trống")
    @Positive(message = "ID phiên sạc phải là số dương")
    private Long sessionId;

}
