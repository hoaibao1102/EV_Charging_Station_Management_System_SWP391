package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.SlotAvailabilityCreateRequest;
import com.swp391.gr3.ev_management.DTO.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.SlotAvailabilityMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotAvailabilityServiceImpl implements SlotAvailabilityService {

    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final ChargingPointRepository chargingPointRepository;   // ✅ thêm
    private final SlotAvailabilityMapper mapper;

    @Override
    @Transactional
    public List<SlotAvailabilityResponse> createForTemplates(SlotAvailabilityCreateRequest req) {
        if (req.getTemplateIds() == null || req.getTemplateIds().isEmpty()) {
            throw new ErrorException("templateIds không được rỗng");
        }
        if (req.getConnectorTypeIds() == null || req.getConnectorTypeIds().isEmpty()) {
            throw new ErrorException("connectorTypeIds không được rỗng");
        }

        List<SlotTemplate> templates = slotTemplateRepository.findAllById(req.getTemplateIds());
        if (templates.isEmpty()) return Collections.emptyList();

        // load connector types
        List<ConnectorType> connectorTypes = connectorTypeRepository.findAllById(req.getConnectorTypeIds());
        if (connectorTypes.isEmpty()) return Collections.emptyList();

        List<SlotAvailability> toSave = new ArrayList<>();

        for (SlotTemplate template : templates) {
            // Lấy station từ template -> config -> station (giả định các field tồn tại)
            Long stationId = Optional.ofNullable(template.getConfig())
                    .map(SlotConfig::getStation)
                    .map(ChargingStation::getStationId)
                    .orElseThrow(() -> new ErrorException(
                            "Template " + template.getTemplateId() + " không có liên kết Station qua Config"));

            // Chuẩn hóa ngày về 00:00:00 cùng ngày của template.startTime
            LocalDateTime date = template.getStartTime()
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);

            for (ConnectorType ct : connectorTypes) {
                // Lấy tất cả charging points của station với connector type ct
                List<ChargingPoint> points =
                        chargingPointRepository.findByStation_StationIdAndConnectorType_ConnectorTypeId(
                                stationId, ct.getConnectorTypeId()
                        );

                for (ChargingPoint point : points) {
                    boolean exists = slotAvailabilityRepository
                            .existsByTemplate_TemplateIdAndChargingPoint_PointIdAndDate(
                                    template.getTemplateId(), point.getPointId(), date);

                    if (!exists) {
                        SlotAvailability sa = SlotAvailability.builder()
                                .template(template)
                                .chargingPoint(point)             // ✅ gán chargingPoint
                                .status(SlotStatus.AVAILABLE)
                                .date(date)
                                .build();
                        toSave.add(sa);
                    }
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
        if (config == null) throw new ErrorException("Không tìm thấy SlotConfig id=" + configId);

        // Lấy các template thuộc config trong khoảng ngày [start, end)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        List<SlotTemplate> templates = slotTemplateRepository
                .findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);

        if (templates.isEmpty()) return Collections.emptyList();

        // Lấy danh sách connector types có mặt ở station của config
        Long stationId = config.getStation().getStationId();
        List<ConnectorType> connectorTypes =
                connectorTypeRepository.findDistinctByChargingPoints_Station_StationId(stationId);

        if (connectorTypes.isEmpty()) return Collections.emptyList();

        // Gọi lại createForTemplates để tái sử dụng logic (repo đã dùng chargingPoint)
        SlotAvailabilityCreateRequest req = new SlotAvailabilityCreateRequest();
        req.setTemplateIds(templates.stream().map(SlotTemplate::getTemplateId).toList());
        req.setConnectorTypeIds(connectorTypes.stream().map(ConnectorType::getConnectorTypeId).toList());

        return createForTemplates(req);
    }

    @Override
    @Transactional
    public SlotAvailabilityResponse updateStatus(Long slotAvailabilityId, SlotStatus status) {
        SlotAvailability sa = slotAvailabilityRepository.findById(slotAvailabilityId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy SlotAvailability id=" + slotAvailabilityId));
        sa.setStatus(status);
        return mapper.toResponse(slotAvailabilityRepository.save(sa));
    }

    @Override
    public List<SlotAvailabilityResponse> findByPointId(Long pointId) {
        List<SlotAvailability> slots = slotAvailabilityRepository.findAllByChargingPoint_PointId(pointId);

        if (slots.isEmpty()) {
            throw new ErrorException("Không tìm thấy SlotAvailability cho PointId = " + pointId);
        }

        return slots.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<SlotAvailabilityResponse> findAll() {
        return slotAvailabilityRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
