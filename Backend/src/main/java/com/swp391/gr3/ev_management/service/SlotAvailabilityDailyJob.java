package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service // Đánh dấu đây là 1 Spring Service chạy background job theo lịch
@RequiredArgsConstructor // Tự động inject các field final qua constructor
@Slf4j // Bật logger để in log
public class SlotAvailabilityDailyJob {

    private final SlotConfigRepository slotConfigRepository; // Repository lấy danh sách slot config
    private final SlotAvailabilitySchedulerService schedulerService; // Service sinh SlotAvailability mỗi ngày

    /**
     * Job chạy mỗi 00:00 (giờ Việt Nam) để reset lại tất cả SlotAvailability
     * dựa trên các SlotConfig đang ACTIVE.
     *
     * - Cron: "0 0 0 * * *"
     *   → giây = 0
     *   → phút = 0
     *   → giờ = 0
     *   → mỗi ngày
     *
     * - zone = Asia/Ho_Chi_Minh: đảm bảo chạy theo giờ VN thay vì UTC.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void resetAllActiveConfigsForToday() {

        // 1) Lấy tất cả các SlotConfig đang ACTIVE
        var actives = slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE);

        // Nếu không có config nào active -> không làm gì
        if (actives.isEmpty()) return;

        // 2) Lấy ngày hôm nay (theo server timezone hoặc JVM default)
        LocalDate today = LocalDate.now();

        // 3) Với mỗi config ACTIVE → reset và tạo SlotAvailability mới cho ngày hôm nay
        for (var cfg : actives) {
            // Gọi scheduler service để xóa slot cũ + sinh slot mới
            int created = schedulerService.resetAndCreateForConfigInDate(cfg.getConfigId(), today);

            // 4) Log kết quả (số slot mới được tạo)
            log.info("Regenerated {} availabilities for config {}", created, cfg.getConfigId());
        }
    }
}
