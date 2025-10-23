package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.SlotAvailabilityCreateRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.entity.SlotTemplate;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.mapper.SlotAvailabilityMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotAvailabilityServiceImpl implements SlotAvailabilityService {

    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final ConnectorTypeRepository connectorTypeRepository; // bạn cần repo này
    private final SlotAvailabilityMapper mapper;

    @Override
    @Transactional
    public List<SlotAvailabilityResponse> createForTemplates(SlotAvailabilityCreateRequest req) {
        if (req.getTemplateIds() == null || req.getTemplateIds().isEmpty()) {
            throw new IllegalArgumentException("templateIds không được rỗng");
        }
        if (req.getConnectorTypeIds() == null || req.getConnectorTypeIds().isEmpty()) {
            throw new IllegalArgumentException("connectorTypeIds không được rỗng");
        }

        List<SlotTemplate> templates = slotTemplateRepository.findAllById(req.getTemplateIds());
        if (templates.isEmpty()) return Collections.emptyList();

        // load connector types
        List<ConnectorType> connectorTypes = connectorTypeRepository.findAllById(req.getConnectorTypeIds());

        List<SlotAvailability> toSave = new ArrayList<>();
        for (SlotTemplate template : templates) {
            // date = ngày của template (lưu ý: cột DB là DATE => có thể đổi sang LocalDate cho chuẩn)
            LocalDateTime date = template.getStartTime().withHour(0).withMinute(0).withSecond(0).withNano(0);

            for (ConnectorType ct : connectorTypes) {
                boolean exists = slotAvailabilityRepository
                        .existsByTemplate_TemplateIdAndConnectorType_ConnectorTypeIdAndDate(
                                template.getTemplateId(), ct.getConnectorTypeId(), date);

                if (!exists) {
                    SlotAvailability sa = SlotAvailability.builder()
                            .template(template)
                            .connectorType(ct)
                            .status(SlotStatus.AVAILABLE)
                            .date(date)
                            .build();
                    toSave.add(sa);
                }
            }
        }

        return slotAvailabilityRepository.saveAll(toSave)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SlotAvailabilityResponse> createForConfigInDate(Long configId, LocalDate date) {
        SlotConfig config = slotConfigRepository.findByConfigId(configId);
        if (config == null) throw new IllegalArgumentException("Không tìm thấy SlotConfig id=" + configId);

        // Lấy tất cả template của config trong ngày (đã có khi bạn generate templates)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        List<SlotTemplate> templates = slotTemplateRepository
                .findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);

        if (templates.isEmpty()) return Collections.emptyList();

        // Lấy tất cả ConnectorType thuộc station của config
        Long stationId = config.getStation().getStationId();

        // ⚠️ tuỳ entity của bạn. Ví dụ nếu ConnectorType có field "point" → "point.station.stationId"
        List<ConnectorType> connectorTypes =
                connectorTypeRepository.findDistinctByChargingPoints_Station_StationId(stationId);

        // Nếu repo của bạn khác tên field, đổi sang method phù hợp (vd: findByChargingPoint_Station_StationId)

        List<Integer> connectorTypeIds = connectorTypes.stream()
                .map(ConnectorType::getConnectorTypeId).toList();

        SlotAvailabilityCreateRequest req = new SlotAvailabilityCreateRequest();
        req.setTemplateIds(templates.stream().map(SlotTemplate::getTemplateId).toList());
        req.setConnectorTypeIds(connectorTypeIds);

        return createForTemplates(req);
    }

    @Override
    @Transactional
    public SlotAvailabilityResponse updateStatus(Long slotAvailabilityId, SlotStatus status) {
        SlotAvailability sa = slotAvailabilityRepository.findById(slotAvailabilityId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy SlotAvailability id=" + slotAvailabilityId));
        sa.setStatus(status);
        return mapper.toResponse(slotAvailabilityRepository.save(sa));
    }

    @Override
    public SlotAvailabilityResponse findById(Long slotAvailabilityId) {
        return mapper.toResponse(
                slotAvailabilityRepository.findById(slotAvailabilityId)
                        .orElseThrow(() -> new NoSuchElementException("Không tìm thấy SlotAvailability id=" + slotAvailabilityId))
        );

    }
}
