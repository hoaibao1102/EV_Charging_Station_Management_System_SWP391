package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.DTO.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import org.springframework.stereotype.Component;

@Component
public class ChargingStationMapper {

    public ChargingStation toEntity(ChargingStationRequest req) {
        if (req == null) return null;
        ChargingStation cs = new ChargingStation();
        cs.setStationName(req.getStationName());
        cs.setAddress(req.getAddress());
        cs.setLatitude(req.getLatitude());
        cs.setLongitude(req.getLongitude());
        cs.setOperatingHours(req.getOperatingHours());
        cs.setStatus(req.getStatus());
        cs.setCreatedAt(req.getCreatedAt());
        return cs;
    }

    public void updateEntity(ChargingStation entity, ChargingStationRequest req) {
        if (entity == null || req == null) return;
        entity.setStationName(req.getStationName());
        entity.setAddress(req.getAddress());
        entity.setLatitude(req.getLatitude());
        entity.setLongitude(req.getLongitude());
        entity.setOperatingHours(req.getOperatingHours());
        entity.setStatus(req.getStatus());
        // Giữ nguyên createdAt (tránh đổi lịch sử); bỏ dòng dưới nếu muốn cho phép cập nhật
        // entity.setCreatedAt(req.getCreatedAt());
    }

    public ChargingStationResponse toResponse(ChargingStation cs) {
        if (cs == null) return null;
        ChargingStationResponse res = new ChargingStationResponse();
        res.setStationId(cs.getStationId());
        res.setStationName(cs.getStationName());
        res.setAddress(cs.getAddress());
        res.setLatitude(cs.getLatitude());
        res.setLongitude(cs.getLongitude());
        res.setOperatingHours(cs.getOperatingHours());
        res.setStatus(cs.getStatus());
        res.setCreatedAt(cs.getCreatedAt());
        return res;
    }
}
