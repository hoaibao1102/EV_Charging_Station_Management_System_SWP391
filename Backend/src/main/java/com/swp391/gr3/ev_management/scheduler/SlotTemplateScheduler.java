package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import com.swp391.gr3.ev_management.service.SlotConfigService;
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
    private final SlotConfigService slotConfigService;     // 👉 Dùng service này để generate cả template + slot
    private final SlotTemplateService slotTemplateService; // 👉 Dùng để kiểm tra hôm nay đã có template chưa

    /**
     * 1) Chạy MỖI GIỜ (00 phút mỗi giờ)
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoEnsureTodayTemplatesBySchedule() {
        log.info("⏰ Scheduled check: ensure today templates & slots exist");
        ensureTodayTemplatesAndSlots();
    }

    /**
     * 2) Chạy NGAY sau khi app khởi động xong
     */
    @EventListener(ApplicationReadyEvent.class)
    public void autoEnsureTodayTemplatesOnStartup() {
        log.info("🚀 App started: ensure today templates & slots exist");
        ensureTodayTemplatesAndSlots();
    }

    /**
     * Hàm dùng chung:
     * - Nếu hôm nay CHƯA có SlotTemplate cho config → gọi SlotConfigService.generateDailyTemplates
     *   => tạo cả Template + SlotAvailability (nếu bạn đã code như vậy trong service)
     */
    private void ensureTodayTemplatesAndSlots() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1).minusNanos(1);

        log.info("🕛 Ensuring templates & slots for date {}", todayStart.toLocalDate());

        slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE).forEach(config -> {
            Long configId = config.getConfigId();
            try {
                // Kiểm tra hôm nay đã có SlotTemplate chưa
                boolean hasTodayTemplate =
                        !slotTemplateService
                                .findByConfig_ConfigIdAndStartTimeBetween(configId, todayStart, todayEnd)
                                .isEmpty();

                if (!hasTodayTemplate) {
                    // ❗Chưa có template hôm nay → gọi generateDailyTemplates() của SlotConfigService
                    // 👉 Hàm này bên bạn đang generate cả Template + SlotAvailability
                    slotConfigService.generateDailyTemplates(configId, todayStart);

                    log.info("✅ Generated templates & slots for config {} on {}",
                            configId, todayStart.toLocalDate());
                } else {
                    log.info("ℹ️ Templates already exist for config {} on {}. Skip.",
                            configId, todayStart.toLocalDate());
                }
            } catch (Exception e) {
                log.error("❌ Failed to ensure templates & slots for config {}: {}",
                        configId, e.getMessage(), e);
            }
        });
    }
}
