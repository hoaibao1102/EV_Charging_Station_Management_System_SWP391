package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChargingSessionResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double energyKWh;
    private int durationMinutes;
    private double cost;
    private ChargingSessionStatus status;
    private Invoice invoice;
}
