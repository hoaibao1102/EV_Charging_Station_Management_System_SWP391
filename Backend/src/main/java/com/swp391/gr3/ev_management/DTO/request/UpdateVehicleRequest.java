package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class UpdateVehicleRequest {
    private Long modelId;       // optional
    private String licensePlate; // optional
}
