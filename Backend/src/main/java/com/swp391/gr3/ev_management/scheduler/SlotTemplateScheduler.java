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

@Component                               // 🧩 Đánh dấu class là một Spring Bean để Scheduler có thể chạy
@RequiredArgsConstructor                 // 🛠️ Lombok tự tạo constructor cho các final field
@Slf4j                                   // 📝 Tự tạo logger phục vụ log debug / info
public class SlotTemplateScheduler {

    private final SlotTemplateService slotTemplateService;     // Service để tạo SlotTemplate hằng ngày
    private final SlotConfigRepository slotConfigRepository;   // Repo để lấy danh sách Config đang ACTIVE

    // 🕛 Scheduler chạy mỗi ngày lúc 00:00:00 theo giờ Việt Nam
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoGenerateTodayTemplates() {
        // Lấy mốc thời gian đầu ngày hôm nay (00:00)
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        log.info("🕛 Auto-generating slot templates for date {}", todayStart.toLocalDate());

        // 🔥 Lấy tất cả các SlotConfig có trạng thái ACTIVE
        slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE).forEach(config -> {
            try {
                /**
                 * Gọi service generateDailyTemplates():
                 *  - configId: ID của SlotConfig cần tạo slot template
                 *  - forDate: ngày cần tạo (truyền todayStart)
                 *  - endDate: tham số thứ 3 nhưng logic hiện tại không sử dụng → truyền cùng giá trị
                 */
                slotTemplateService.generateDailyTemplates(config.getConfigId(), todayStart, todayStart);

                log.info("✅ Generated slots for config {}", config.getConfigId());
            } catch (Exception e) {
                // Nếu có lỗi, log lỗi chi tiết để tiện debug
                log.error("❌ Failed to generate slots for config {}: {}", config.getConfigId(), e.getMessage(), e);
            }
        });
    }
}
