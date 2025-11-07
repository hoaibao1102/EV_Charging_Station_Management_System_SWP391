package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private VehicleModelStatus status;
}
