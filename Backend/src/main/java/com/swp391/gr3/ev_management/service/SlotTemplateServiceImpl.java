package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDate forDate) {
        SlotConfig config = slotConfigRepository.findByConfigId(configId);
        if (config == null) {
            throw new IllegalArgumentException("Không tìm thấy SlotConfig với id = " + configId);
        }

        int duration = config.getSlotDurationMin();
        if (duration <= 0) {
            throw new IllegalArgumentException("slotDurationMin phải > 0.");
        }

        // activeFrom–activeExpire là KHUNG GIỜ TRONG NGÀY
        if (config.getActiveFrom() == null || config.getActiveExpire() == null) {
            throw new IllegalArgumentException("Cần thiết lập activeFrom và activeExpire trong SlotConfig.");
        }
        LocalTime fromTime = config.getActiveFrom().toLocalTime();
        LocalTime toTime   = config.getActiveExpire().toLocalTime();

        if (!toTime.isAfter(fromTime)) {
            // Vì reset sau 23:59, nên toTime phải > fromTime cùng ngày
            throw new IllegalArgumentException("activeExpire (giờ trong ngày) phải > activeFrom.");
        }

        LocalDateTime windowStart = forDate.atTime(fromTime);
        LocalDateTime windowEnd   = forDate.atTime(toTime); // exclusive

        long totalMinutes = Duration.between(windowStart, windowEnd).toMinutes();
        if (totalMinutes <= 0) {
            throw new IllegalArgumentException("Khoảng thời gian trong ngày không hợp lệ.");
        }
        if (totalMinutes % duration != 0) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian (" + totalMinutes + " phút) không chia hết cho slotDurationMin = " + duration
            );
        }

        // Dọn templates trong cửa sổ giờ của ngày này
        slotTemplateRepository.deleteByConfig_ConfigIdAndStartTimeBetween(configId, windowStart, windowEnd);

        int slots = (int) (totalMinutes / duration);
        List<SlotTemplate> toSave = new ArrayList<>(slots);

        for (int i = 0; i < slots; i++) {
            LocalDateTime start = windowStart.plusMinutes((long) i * duration);
            LocalDateTime end   = start.plusMinutes(duration);

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
    public List<SlotTemplateResponse> generateTemplatesForRange(Long configId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        List<SlotTemplateResponse> all = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            all.addAll(generateDailyTemplates(configId, d)); // sau 23:59 sẽ reset tự nhiên cho ngày kế tiếp
        }
        return all;
    }

    @Override
    public SlotTemplateResponse getById(Long templateId) {
        return  mapper.toResponse(slotTemplateRepository.findById(templateId).orElse(null));
    }
}
