package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import org.springframework.stereotype.Component;

@Component
public class SlotAvailabilityMapper {

    public SlotAvailabilityResponse toResponse(SlotAvailability entity) {
        if (entity == null) return null;
        return SlotAvailabilityResponse.builder()
                .slotId(entity.getSlotId())
                .templateId(entity.getTemplate() != null ? entity.getTemplate().getTemplateId() : null)
                .pointId(entity.getChargingPoint().getPointId() != null ? entity.getChargingPoint().getPointId() : null)
                .status(entity.getStatus())
                .date(entity.getDate())
                .build();
    }
}
