package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.entity.SlotTemplate;
import com.swp391.gr3.ev_management.mapper.SlotTemplateMapper;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import com.swp391.gr3.ev_management.repository.SlotTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotTemplateServiceImpl implements SlotTemplateService {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final SlotTemplateMapper mapper;

    @Override
    @Transactional
    public List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDateTime forDate, LocalDateTime endDate) {
        SlotConfig config = slotConfigRepository.findByConfigId(configId);
        if (config == null) {
            throw new IllegalArgumentException("Không tìm thấy SlotConfig với id = " + configId);
        }

        int duration = config.getSlotDurationMin();
        if (duration <= 0) {
            throw new IllegalArgumentException("slotDurationMin phải > 0.");
        }

        // Khung giờ cố định: 00:00 -> 24:00 (exclusive = 00:00 ngày kế tiếp)
        LocalDateTime windowStart = forDate.toLocalDate().atStartOfDay();           // 00:00
        LocalDateTime windowEndExclusive = windowStart.plusDays(1);                 // 24:00 (exclusive)
        long totalMinutes = Duration.between(windowStart, windowEndExclusive).toMinutes(); // 1440

        if (totalMinutes % duration != 0) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian 24 giờ (" + totalMinutes + " phút) không chia hết cho slotDurationMin = " + duration
            );
        }

        // Xóa các template trong ngày này (inclusive), tránh đè 00:00 của ngày hôm sau
        slotTemplateRepository.deleteByConfig_ConfigIdAndStartTimeBetween(
                configId,
                windowStart,
                windowEndExclusive.minusNanos(1)
        );

        int slots = (int) (totalMinutes / duration);
        List<SlotTemplate> toSave = new ArrayList<>(slots);

        for (int i = 0; i < slots; i++) {
            LocalDateTime start = windowStart.plusMinutes((long) i * duration);
            LocalDateTime end = start.plusMinutes(duration); // luôn <= windowEndExclusive do đã kiểm tra chia hết

            SlotTemplate template = SlotTemplate.builder()
                    .slotIndex(i + 1)
                    .startTime(start)
                    .endTime(end)
                    .config(config)
                    .build();
            toSave.add(template);
        }

        return slotTemplateRepository.saveAll(toSave)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SlotTemplateResponse> generateTemplatesForRange(Long configId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        List<SlotTemplateResponse> all = new ArrayList<>();
        for (LocalDateTime d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            all.addAll(generateDailyTemplates(configId, d, endDate)); // sau 23:59 sẽ reset tự nhiên cho ngày kế tiếp
        }
        return all;
    }

    @Override
    public SlotTemplateResponse getById(Long templateId) {
        return  mapper.toResponse(slotTemplateRepository.findById(templateId).orElse(null));
    }

    @Override
    public List<SlotTemplateResponse> getAll() {
        return slotTemplateRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
