package com.swp391.gr3.ev_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffCreateRequest {

    @NotNull(message = "ID loại đầu nối là bắt buộc")
    @Positive(message = "ID loại đầu nối phải lớn hơn 0")
    private Long connectorTypeId;

    @NotNull(message = "Giá theo kWh là bắt buộc")
    @Positive(message = "Giá theo kWh phải lớn hơn 0")
    private double pricePerKWh;

    @NotNull(message = "Giá theo phút là bắt buộc")
    @Positive(message = "Giá theo phút phải lớn hơn 0")
    private double pricePerMin;

    @NotBlank(message = "Đơn vị tiền tệ không được để trống")
    @Size(max = 10, message = "Đơn vị tiền tệ không được vượt quá 10 ký tự")
    private String currency;

    @NotNull(message = "Ngày bắt đầu hiệu lực là bắt buộc")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "Ngày kết thúc hiệu lực là bắt buộc")
    private LocalDateTime effectiveTo;
}
