package com.swp391.gr3.ev_management.DTO.request;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Slot duration cannot be null")
    private int slotDurationMin;
    @NotNull(message = "Station ID cannot be null")
    private Long stationId;
    @NotBlank(message = "Time zone cannot be blank")
    private LocalDateTime activeFrom;
    private LocalDateTime activeExpire;
    private SlotConfigStatus isActive;
}
