package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "ID đặt lịch không được để trống")
    @Positive(message = "ID đặt lịch phải là số dương")
    private Long bookingId;

    @NotNull(message = "ID trạm sạc không được để trống")
    @Positive(message = "ID trạm sạc phải là số dương")
    private Long stationId;

    @NotNull(message = "ID xe không được để trống")
    @Positive(message = "ID xe phải là số dương")
    private Long vehicleId;

    @NotNull(message = "Thời gian đặt lịch không được để trống")
    private LocalDateTime bookingTime;

    @NotNull(message = "Thời gian bắt đầu dự kiến không được để trống")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "Thời gian kết thúc dự kiến không được để trống")
    private LocalDateTime scheduledEndTime;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;
}
