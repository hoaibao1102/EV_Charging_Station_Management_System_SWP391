package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelUpdateRequest {
    private String brand; // optional
    private String model; // optional
    @Min(value = 1886, message = "Year must be realistic")
    private Integer year; // optional
    private String imageUrl; // optional
    private Long connectorTypeId; // optional
}

