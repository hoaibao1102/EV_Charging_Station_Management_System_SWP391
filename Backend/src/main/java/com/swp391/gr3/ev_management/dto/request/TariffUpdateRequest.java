package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TariffUpdateRequest {

    @NotNull(message = "ID loại đầu nối không được để trống")
    @Positive(message = "ID loại đầu nối phải là số dương")
    private Long connectorTypeId;

    @NotNull(message = "Giá theo kWh không được để trống")
    @Positive(message = "Giá theo kWh phải lớn hơn 0")
    private Double pricePerKWh;

    @NotNull(message = "Giá theo phút không được để trống")
    @Positive(message = "Giá theo phút phải lớn hơn 0")
    private Double pricePerMin;

    @NotNull(message = "Đơn vị tiền tệ không được để trống")
    @Size(max = 10, message = "Đơn vị tiền tệ không được vượt quá 10 ký tự")
    private String currency;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
