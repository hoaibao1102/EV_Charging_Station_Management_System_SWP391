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

    @NotNull(message = "ConnectorTypeId is required")
    @Positive(message = "ConnectorTypeId must be greater than 0")
    private Long connectorTypeId;

    @NotNull(message = "Price per kWh is required")
    @Positive(message = "Price per kWh must be greater than 0")
    private double pricePerKWh;

    @NotNull(message = "Price per minute is required")
    @Positive(message = "Price per minute must be greater than 0")
    private double pricePerMin;

    @NotBlank(message = "Currency cannot be blank")
    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "Effective to date is required")
    private LocalDateTime effectiveTo;
}
