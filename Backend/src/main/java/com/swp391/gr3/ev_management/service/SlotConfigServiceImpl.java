package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.mapper.SlotConfigMapper;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotConfigServiceImpl implements SlotConfigService {

    private final SlotConfigRepository slotConfigRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final SlotConfigMapper mapper;

    @Override
    public SlotConfigResponse findByConfigId(Long slotConfigId) {
        SlotConfig slotConfig = slotConfigRepository.findByConfigId(slotConfigId);
        return mapper.toResponse(slotConfig);
    }

    @Override
    public SlotConfigResponse findByStation_StationId(Long stationId) {
        SlotConfig slotConfig = slotConfigRepository.findByStation_StationId(stationId);
        return mapper.toResponse(slotConfig);
    }

    @Override
    public List<SlotConfigResponse> findAll() {
        return slotConfigRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SlotConfigResponse addSlotConfig(SlotConfigRequest req) {
        ChargingStation station = chargingStationRepository.findByStationId(req.getStationId());
        if (station == null) {
            throw new IllegalArgumentException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }
        SlotConfig entity = mapper.toEntity(req, station);
        SlotConfig saved = slotConfigRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    public SlotConfigResponse updateSlotConfig(Long configId, SlotConfigRequest req) {
        SlotConfig existing = slotConfigRepository.findByConfigId(configId);
        if (existing == null) return null;

        ChargingStation station = chargingStationRepository.findByStationId(req.getStationId());
        if (station == null) {
            throw new IllegalArgumentException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }

        mapper.updateEntity(existing, req, station);
        SlotConfig updated = slotConfigRepository.save(existing);
        return mapper.toResponse(updated);
    }
}
