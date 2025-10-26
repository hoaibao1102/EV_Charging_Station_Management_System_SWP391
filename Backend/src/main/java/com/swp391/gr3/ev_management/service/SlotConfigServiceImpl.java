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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotConfigServiceImpl implements SlotConfigService {

    private final SlotConfigRepository slotConfigRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final SlotConfigMapper mapper;

    // ✅ Thêm các service đúng nơi để generate
    private final SlotTemplateService slotTemplateService;
    // Optional: nếu muốn tạo Availability ngay sau khi tạo Template
    private final SlotAvailabilityService slotAvailabilityService;

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
    @Transactional
    public SlotConfigResponse addSlotConfig(SlotConfigRequest req) {
        ChargingStation station = chargingStationRepository.findByStationId(req.getStationId());
        if (station == null) {
            throw new IllegalArgumentException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }
        SlotConfig entity = mapper.toEntity(req, station);
        SlotConfig saved = slotConfigRepository.save(entity);

        // ❌ BỎ: slotConfigRepository.generateDailyTemplates(...)
        // ✅ ĐÚNG:
        generateDailyTemplates(saved.getConfigId(), LocalDateTime.now());

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SlotConfigResponse updateSlotConfig(Long configId, SlotConfigRequest req) {
        SlotConfig existing = slotConfigRepository.findByConfigId(configId);
        if (existing == null) return null;

        ChargingStation station = chargingStationRepository.findByStationId(req.getStationId());
        if (station == null) {
            throw new IllegalArgumentException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }

        mapper.updateEntity(existing, req, station);
        SlotConfig updated = slotConfigRepository.save(existing);

        // Tuỳ nhu cầu: có thể regenerate template cho hôm nay nếu activeFrom/activeExpire thay đổi
        // generateDailyTemplates(updated.getConfigId(), LocalDateTime.now());

        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void generateDailyTemplates(Long configId, LocalDateTime now) {
        // 1) Generate SlotTemplates cho hôm nay (service chuyên trách)
        slotTemplateService.generateDailyTemplates(configId, now);

        // 2) (Tuỳ chọn) Tạo luôn SlotAvailability cho hôm nay
        //    Nếu muốn tự động upsert availability dựa trên connector/charging points hiện có:
        if (slotAvailabilityService != null) {
            slotAvailabilityService.createForConfigInDate(configId, now.toLocalDate());
        }
    }
}
