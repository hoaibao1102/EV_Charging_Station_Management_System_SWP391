package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.SlotStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SlotAvailabilityResponse {
    private Long slotId;
    private Long templateId;
    private Integer connectorTypeId;
    private SlotStatus status;
    private LocalDateTime date; // hoặc LocalDate tuỳ bạn
}
