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

    // 🕛 Chạy tự động mỗi ngày lúc 00:00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void autoGenerateTodayTemplates() {
        LocalDateTime today = LocalDateTime.now();
        log.info("🕛 Auto-generating slot templates for {}", today);

        // 🔥 Chỉ lấy những config đang active
        slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE).forEach(config -> {
            try {
                slotTemplateService.generateDailyTemplates(config.getConfigId(), today);
                log.info("✅ Generated slots for config {}", config.getConfigId());
            } catch (Exception e) {
                log.error("❌ Failed to generate slots for config {}: {}", config.getConfigId(), e.getMessage());
            }
        });
    }
}
