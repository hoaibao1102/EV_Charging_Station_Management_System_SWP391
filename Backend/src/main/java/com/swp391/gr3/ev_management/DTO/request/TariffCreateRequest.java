package com.swp391.gr3.ev_management.DTO.request;

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
    @NotNull(message = "ConnectorTypeId không được để trống")
    private Long connectorTypeId;

    @NotNull(message = "Giá tiền không được để trống")
    @Positive(message = "Giá tiền phải lớn hơn 0")
    private double pricePerKWh;

    @NotBlank(message = "Currency không được để trống")
    @Size(max = 10, message = "Currency không được quá 10 ký tự")
    private String currency;

    @NotNull(message = "Effective from không được để trống")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "Effective to không được để trống")
    private LocalDateTime effectiveTo;
}
