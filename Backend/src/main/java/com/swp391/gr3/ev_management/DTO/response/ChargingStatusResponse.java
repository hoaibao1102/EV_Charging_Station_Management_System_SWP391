package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargingStatusResponse {
    private Long sessionId;
    private double currentSoc; // mức pin hiện tại (%)
    private double energyKWh;  // điện năng đã nạp (tuỳ chọn)
    private long minutesElapsed;
}
