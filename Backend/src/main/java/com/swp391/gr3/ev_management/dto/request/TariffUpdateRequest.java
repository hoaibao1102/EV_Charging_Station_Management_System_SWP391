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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TariffUpdateRequest {

    @NotNull(message = "Connector Type ID cannot be null")
    @Positive(message = "Connector Type ID must be positive")
    private Long connectorTypeId;

    @NotNull(message = "Tariff Name cannot be null")
    @Positive(message = "Tariff Name must be positive")
    private double pricePerKWh;

    @NotNull(message = "Tariff Name cannot be null")
    @Positive
    private double pricePerMin;

    @NotNull(message = "Tariff Name cannot be null")
    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
