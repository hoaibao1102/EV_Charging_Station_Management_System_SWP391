package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import org.springframework.stereotype.Component;

@Component
public class SlotConfigMapper {

    // Tạo entity mới từ request + trạm đã load
    public SlotConfig toEntity(SlotConfigRequest req, ChargingStation station) {
        if (req == null) return null;
        SlotConfig sc = new SlotConfig();
        sc.setStation(station);
        sc.setSlotDurationMin(req.getSlotDurationMin());
        sc.setActiveFrom(req.getActiveFrom());
        sc.setActiveExpire(req.getActiveExpire());
        sc.setIsActive(req.getIsActive());
        return sc;
    }

    // Cập nhật entity hiện có từ request + trạm đã load
    public void updateEntity(SlotConfig entity, SlotConfigRequest req, ChargingStation station) {
        if (entity == null || req == null) return;
        entity.setStation(station);
        entity.setSlotDurationMin(req.getSlotDurationMin());
        entity.setActiveFrom(req.getActiveFrom());
        entity.setActiveExpire(req.getActiveExpire());
        entity.setIsActive(req.getIsActive());
    }

    // Map entity -> response (trả thẳng ChargingStation trong response theo yêu cầu)
    public SlotConfigResponse toResponse(SlotConfig entity) {
        if (entity == null) return null;
        return SlotConfigResponse.builder()
                .configId(entity.getConfigId())
                .slotDurationMin(entity.getSlotDurationMin())
                .stationId(entity.getConfigId()) // Trả nguyên object ChargingStation
                .activeFrom(entity.getActiveFrom())
                .activeExpire(entity.getActiveExpire())
                .isActive(entity.getIsActive())
                .build();
    }
}
