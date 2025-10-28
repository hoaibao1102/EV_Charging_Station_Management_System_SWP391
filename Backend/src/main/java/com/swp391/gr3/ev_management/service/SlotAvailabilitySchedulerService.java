package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotAvailabilitySchedulerService {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final ChargingPointRepository chargingPointRepository;

    /**
     * Reset toàn bộ SlotAvailability của 1 config trong NGÀY (xóa rồi tạo lại theo templates & points hiện có).
     * YÊU CẦU: đã có templates cho ngày đó.
     */
    @Transactional
    public int resetAndCreateForConfigInDate(Long configId, LocalDate date) {
        var config = slotConfigRepository.findByConfigId(configId);
        if (config == null) {
            throw new IllegalArgumentException("Không tìm thấy SlotConfig id=" + configId);
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        // 1) XÓA AVAILABILITY TRONG NGÀY (phải xóa con trước khi có ý định đụng tới template)
        slotAvailabilityRepository.deleteByTemplate_Config_ConfigIdAndDateBetween(configId, start, end);

        // 2) LẤY TEMPLATES TRONG NGÀY
        var templates = slotTemplateRepository.findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);
        if (templates == null || templates.isEmpty()) {
            return 0; // chưa có template => không tạo được availability
        }

        // 3) LẤY TOÀN BỘ POINTS CỦA STATION (đủ vì mỗi point có 1 connectorType)
        Long stationId = config.getStation().getStationId();
        List<com.swp391.gr3.ev_management.entity.ChargingPoint> points =
                chargingPointRepository.findByStation_StationId(stationId);

        if (points == null || points.isEmpty()) {
            return 0; // không có điểm sạc => không tạo được availability
        }

        // 4) TẠO LẠI AVAILABILITY (mỗi template x mỗi point)
        var toSave = new ArrayList<SlotAvailability>(templates.size() * points.size());
        for (var t : templates) {
            LocalDateTime day = t.getStartTime().withHour(0).withMinute(0).withSecond(0).withNano(0);
            for (var p : points) {
                toSave.add(SlotAvailability.builder()
                        .template(t)
                        .chargingPoint(p)
                        .status(SlotStatus.AVAILABLE)
                        .date(day)
                        .build());
            }
        }

        // 5) LƯU HÀNG LOẠT
        return slotAvailabilityRepository.saveAll(toSave).size();
    }
}
