package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotConfigRequest {
    private int slotDurationMin;
    private Long stationId;
    private LocalDateTime activeFrom;
    private LocalDateTime activeExpire;
    private SlotConfigStatus isActive;
}
