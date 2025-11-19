package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import com.swp391.gr3.ev_management.service.SlotTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlotTemplateScheduler {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotTemplateService slotTemplateService; // 👉 CHỈ làm việc với Template

    /**
     * 1) Chạy MỖI GIỜ (00 phút mỗi giờ)
     *    → Đảm bảo hôm nay có SlotTemplate cho tất cả SlotConfig ACTIVE
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoEnsureTodayTemplatesBySchedule() {
        log.info("⏰ Scheduled check: ensure today slot templates exist");
        ensureTodayTemplates();
    }

    /**
     * 2) Chạy NGAY sau khi app khởi động xong
     */
    @EventListener(ApplicationReadyEvent.class)
    public void autoEnsureTodayTemplatesOnStartup() {
        log.info("🚀 App started: ensure today slot templates exist");
        ensureTodayTemplates();
    }

    /**
     * Hàm dùng chung:
     * - Nếu hôm nay CHƯA có SlotTemplate cho config → gọi SlotTemplateService.generateDailyTemplates
     * - KHÔNG tạo SlotAvailability ở đây.
     */
    private void ensureTodayTemplates() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1).minusNanos(1);

        log.info("🕛 Ensuring slot templates for date {}", todayStart.toLocalDate());

        slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE).forEach(config -> {
            Long configId = config.getConfigId();
            try {
                boolean hasTodayTemplate =
                        !slotTemplateService
                                .findByConfig_ConfigIdAndStartTimeBetween(configId, todayStart, todayEnd)
                                .isEmpty();

                if (!hasTodayTemplate) {
                    // 👉 Chỉ generate TEMPLATE, không động đến SlotAvailability
                    slotTemplateService.generateDailyTemplates(configId, todayStart, todayEnd);

                    log.info("✅ Generated slot templates for config {} on {}",
                            configId, todayStart.toLocalDate());
                } else {
                    log.info("ℹ️ Slot templates already exist for config {} on {}. Skip.",
                            configId, todayStart.toLocalDate());
                }
            } catch (Exception e) {
                log.error("❌ Failed to ensure slot templates for config {}: {}",
                        configId, e.getMessage(), e);
            }
        });
    }
}
