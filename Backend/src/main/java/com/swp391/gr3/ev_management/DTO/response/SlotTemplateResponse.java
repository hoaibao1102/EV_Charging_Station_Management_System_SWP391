package com.swp391.gr3.ev_management.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotTemplateResponse {
    private Long templateId;
    private int slotIndex;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long configId;
}
