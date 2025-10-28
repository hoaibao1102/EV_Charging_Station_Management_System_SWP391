package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelResponse {
    private Long modelId;
    private String brand;
    private String model;
    private int year;
    private String imageUrl;
    private Long connectorTypeId;
    private String connectorTypeCode;
    private String connectorTypeDisplayName;
    private double connectorDefaultMaxPowerKW;
    private double batteryCapacityKWh;
}

