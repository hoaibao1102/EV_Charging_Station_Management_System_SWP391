package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotTemplateDailyJob {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotTemplateService slotTemplateService;

    // Chạy 00:00:00 hàng ngày theo giờ VN
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void regenerateTodayTemplatesForActiveConfigs() {
        // Lấy toàn bộ config đang ACTIVE (tùy enum của bạn)
        List<SlotConfig> actives = slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE);
        if (actives.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now(); // hôm nay
        for (SlotConfig cfg : actives) {
            // Optionally: bỏ qua nếu đã hết hạn khung ngày tổng (nếu bạn dùng activeFrom/activeExpire như ngày theo lịch)
            // Ở code hiện tại bạn dùng activeFrom/activeExpire như GIỜ TRONG NGÀY, nên có thể bỏ điều kiện này.

            // ✅ Xóa + tạo mới cho hôm nay
            slotTemplateService.generateDailyTemplates(cfg.getConfigId(), now);
        }
    }
}
