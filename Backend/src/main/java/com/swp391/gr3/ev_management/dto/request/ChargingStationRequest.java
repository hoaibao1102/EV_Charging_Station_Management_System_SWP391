package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingStationRequest {

    @NotBlank(message = "Tên trạm sạc không được để trống")
    private String stationName;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Vĩ độ không được để trống")
    private Double latitude;

    @NotNull(message = "Kinh độ không được để trống")
    private Double longitude;

    @NotBlank(message = "Giờ hoạt động không được để trống")
    private String operatingHours;

    @NotNull(message = "Trạng thái không được để trống")
    private ChargingStationStatus status;

    private LocalDateTime createdAt;
}
