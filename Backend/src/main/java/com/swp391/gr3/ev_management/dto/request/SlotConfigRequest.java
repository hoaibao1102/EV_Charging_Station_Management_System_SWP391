package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotConfigRequest {

    @NotNull(message = "Slot duration cannot be null")
    @Positive(message = "Slot duration must be positive")
    private int slotDurationMin;

    @NotNull(message = "Station ID cannot be null")
    @Positive(message = "Station ID must be positive")
    private Long stationId;

    @NotNull(message = "Time zone cannot be blank")
    private LocalDateTime activeFrom;

    private LocalDateTime activeExpire;

    @NotBlank(message = "Status cannot be blank")
    private SlotConfigStatus isActive;
}
