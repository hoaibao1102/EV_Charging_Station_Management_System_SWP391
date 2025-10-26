package com.swp391.gr3.ev_management.DTO.request;

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
    private Long connectorTypeId;

    @Positive(message = "Giá tiền phải lớn hơn 0")
    private double pricePerKWh;

    @Size(max = 10, message = "Currency không được quá 10 ký tự")
    private String currency;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
