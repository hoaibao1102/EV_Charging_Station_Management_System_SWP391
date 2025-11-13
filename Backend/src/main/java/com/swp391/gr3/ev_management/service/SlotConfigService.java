package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.dto.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface SlotConfigService {
    SlotConfigResponse findByConfigId(Long slotConfigId);
    SlotConfigResponse findByStation_StationId(Long stationId);
    List<SlotConfigResponse> findAll();
    SlotConfigResponse addSlotConfig(SlotConfigRequest slotConfigRequest);
    SlotConfigResponse updateSlotConfig(Long configId, SlotConfigRequest slotConfigRequest);
    void generateDailyTemplates(Long configId, LocalDateTime now);
    SlotConfigResponse deactivateConfig(Long configId);
    SlotConfig findEntityById(Long slotConfigId);

    List<SlotConfig> findByIsActive(SlotConfigStatus slotConfigStatus);
}
