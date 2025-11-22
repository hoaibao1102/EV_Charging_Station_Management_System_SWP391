package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StopCharSessionRequest {

    @NotNull(message = "ID phiên sạc không được để trống")
    @Positive(message = "ID phiên sạc phải là số dương")
    private Long sessionId;

    // Tuỳ chọn: SOC cuối cùng từ frontend (0-100)
    @Min(value = 0, message = "SOC cuối phải lớn hơn hoặc bằng 0")
    @Max(value = 100, message = "SOC cuối phải nhỏ hơn hoặc bằng 100")
    private Integer finalSoc;
}
