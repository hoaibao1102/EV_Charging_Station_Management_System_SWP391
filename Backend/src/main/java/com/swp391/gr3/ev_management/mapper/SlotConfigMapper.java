package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.dto.response.SlotConfigResponse;
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

    // Map entity -> response (trả về stationId, KHÔNG trả nguyên ChargingStation)
    public SlotConfigResponse toResponse(SlotConfig entity) {
        if (entity == null) return null;

        Long stationId = null;
        if (entity.getStation() != null) {
            stationId = entity.getStation().getStationId();
        }

        return SlotConfigResponse.builder()
                .configId(entity.getConfigId())
                .slotDurationMin(entity.getSlotDurationMin())
                .stationId(stationId)
                .activeFrom(entity.getActiveFrom())
                .activeExpire(entity.getActiveExpire())
                .isActive(entity.getIsActive())
                .build();
    }

    // Nếu bạn muốn trả nguyên ChargingStation trong response,
    // hãy thêm field tương ứng trong SlotConfigResponse (vd: ChargingStation station)
    // rồi dùng mapper dưới (và nhớ cập nhật builder trong DTO):
    //
    // public SlotConfigResponse toResponseWithStation(SlotConfig entity) {
    //     if (entity == null) return null;
    //     return SlotConfigResponse.builder()
    //             .configId(entity.getConfigId())
    //             .slotDurationMin(entity.getSlotDurationMin())
    //             .stationId(entity.getStation() != null ? entity.getStation().getStationId() : null)
    //             .station(entity.getStation()) // <— cần field này trong DTO
    //             .activeFrom(entity.getActiveFrom())
    //             .activeExpire(entity.getActiveExpire())
    //             .isActive(entity.getIsActive())
    //             .build();
    // }
}
