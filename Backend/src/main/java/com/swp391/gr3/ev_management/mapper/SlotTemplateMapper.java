package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.entity.SlotTemplate;
import org.springframework.stereotype.Component;

@Component
public class SlotTemplateMapper {

    public SlotTemplateResponse toResponse(SlotTemplate entity) {
        if (entity == null) return null;
        return SlotTemplateResponse.builder()
                .templateId(entity.getTemplateId())
                .slotIndex(entity.getSlotIndex())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .configId(entity.getConfig() != null ? entity.getConfig().getConfigId() : null)
                .build();
    }
}
