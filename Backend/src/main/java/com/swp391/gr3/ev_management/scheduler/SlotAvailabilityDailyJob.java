package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import com.swp391.gr3.ev_management.service.SlotAvailabilitySchedulerService;
import com.swp391.gr3.ev_management.service.SlotAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotAvailabilityDailyJob {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotAvailabilitySchedulerService schedulerService;
    private final SlotAvailabilityService slotAvailabilityService;

    /**
     * 1) MỖI 60 PHÚT kiểm tra:
     *  - Nếu hôm nay CHƯA có SlotAvailability cho config → sinh mới
     *  - Nếu đã có rồi → bỏ qua, không đụng vào booking hiện tại
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void ensureTodayAvailabilitiesBySchedule() {
        log.info("⏰ Scheduled check: ensure today slot availabilities exist");
        ensureTodayAvailabilities();
    }

    /**
     * 2) Ngay sau khi APP KHỞI ĐỘNG xong:
     *  - Cũng chạy logic tương tự (phòng trường hợp app tắt lúc 00:00)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureTodayAvailabilitiesOnStartup() {
        log.info("🚀 App started: ensure today slot availabilities exist");
        ensureTodayAvailabilities();
    }

    /**
     * Hàm dùng chung:
     *  - Duyệt hết các SlotConfig ACTIVE
     *  - Với từng config:
     *      + Nếu TRONG NGÀY HÔM NAY KHÔNG có SlotAvailability nào → gọi resetAndCreateForConfigInDate()
     *      + Nếu ĐÃ CÓ slot hôm nay → KHÔNG reset (tránh mất booking)
     */
    private void ensureTodayAvailabilities() {
        var actives = slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE);
        if (actives.isEmpty()) {
            log.info("No active SlotConfig found. Skip availability generation.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1);

        // 👇 Ngày hôm qua
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd   = yesterdayStart.plusDays(1);

        for (var cfg : actives) {
            Long configId = cfg.getConfigId();
            try {
                // ⚠️ 1) Kiểm tra hôm nay đã có slot chưa
                var todaysSlots = slotAvailabilityService.findByConfigAndDateBetween(
                        configId, todayStart, todayEnd
                );
                boolean hasTodaySlots = !todaysSlots.isEmpty();

                if (!hasTodaySlots) {
                    // 🧹 2) CHƯA có slot hôm nay → trước khi tạo mới, xoá slot của NGÀY HÔM QUA
                    int deletedYesterday = slotAvailabilityService
                            .deleteByTemplate_Config_ConfigIdAndDateBetween(
                                    configId,
                                    yesterdayStart,
                                    yesterdayEnd
                            );
                    log.info("🧹 Deleted {} availabilities for config {} on yesterday {}",
                            deletedYesterday, configId, yesterday);

                    // ✅ 3) Tạo slot cho HÔM NAY
                    int created = schedulerService.resetAndCreateForConfigInDate(configId, today);
                    log.info("✅ Created {} availabilities for config {} on {}",
                            created, configId, today);
                } else {
                    // ĐÃ có slot hôm nay rồi -> KHÔNG làm gì để tránh mất booking
                    log.info("ℹ️ Availabilities already exist for config {} on {}. Skip.",
                            configId, today);
                }
            } catch (Exception e) {
                log.error("❌ Failed to ensure availabilities for config {}: {}",
                        configId, e.getMessage(), e);
            }
        }
    }
}
