package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.service.SlotTemplateService;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlotTemplateScheduler {

    private final SlotTemplateService slotTemplateService;
    private final SlotConfigRepository slotConfigRepository;

    // 🕛 Chạy tự động mỗi ngày lúc 00:00:00 theo giờ VN
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoGenerateTodayTemplates() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay(); // 00:00 hôm nay
        log.info("🕛 Auto-generating slot templates for date {}", todayStart.toLocalDate());

        // 🔥 Chỉ lấy những config đang active
        slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE).forEach(config -> {
            try {
                // generateDailyTemplates hiện yêu cầu 3 tham số: truyền cùng một ngày cho forDate & endDate
                slotTemplateService.generateDailyTemplates(config.getConfigId(), todayStart, todayStart);
                log.info("✅ Generated slots for config {}", config.getConfigId());
            } catch (Exception e) {
                log.error("❌ Failed to generate slots for config {}: {}", config.getConfigId(), e.getMessage(), e);
            }
        });
    }
}
