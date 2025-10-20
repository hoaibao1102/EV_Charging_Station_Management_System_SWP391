package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long vehicleId;
    private Long driverId;
    private String licensePlate;
    private Long modelId;
    private String modelName;
    private String brand;
    private String connectorTypeName;
}
