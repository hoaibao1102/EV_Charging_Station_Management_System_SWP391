package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service // Đánh dấu đây là Service — nơi chứa job chạy theo lịch
@RequiredArgsConstructor // Lombok tạo constructor để inject các dependency final
public class SlotTemplateDailyJob {

    private final SlotConfigService slotConfigService;   // Service để lấy danh sách SlotConfig
    private final SlotTemplateService slotTemplateService; // Service để generate SlotTemplate hằng ngày

    /**
     * Job chạy MỖI NGÀY VÀO 00:00:00 (giờ Việt Nam)
     * Dùng để re-generate SlotTemplate cho ngày hôm nay
     * dành cho các SlotConfig đang ACTIVE.
     *
     * Cron: 0 0 0 * * *
     *  - Giây  = 0
     *  - Phút  = 0
     *  - Giờ   = 0
     *  - Ngày  = *
     *  - Tháng = *
     *  - Thứ   = *
     *
     * zone = "Asia/Ho_Chi_Minh"
     *  → đảm bảo job chạy đúng múi giờ Việt Nam, không bị lệch UTC.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional // Đảm bảo toàn bộ quá trình generate template nằm trong 1 transaction
    public void regenerateTodayTemplatesForActiveConfigs() {

        // 1️⃣ Lấy tất cả các SlotConfig đang ACTIVE
        //    → Mỗi trạm chỉ có 1 config active tại 1 thời điểm (theo logic hệ thống)
        List<SlotConfig> actives = slotConfigService.findByIsActive(SlotConfigStatus.ACTIVE);

        // Nếu không có config nào đang active → không cần làm gì
        if (actives.isEmpty()) return;

        // 2️⃣ Xác định ngày hiện tại (00:00 hôm nay)
        LocalDateTime now = LocalDateTime.now();

        // 3️⃣ Generate lại SlotTemplate cho HÔM NAY cho từng config
        for (SlotConfig cfg : actives) {

            // 🔎 Ghi chú:
            // activeFrom / activeExpire trong hệ thống của bạn đang được dùng như "giờ trong ngày"
            // chứ không phải ngày-range. Vì vậy job này không cần kiểm tra xem config có hết hạn không.
            // Nếu sau này bạn dùng activeFrom/activeExpire để quản lý theo NGÀY thì thêm điều kiện vào.

            // 4️⃣ Gọi service sinh template hằng ngày (xóa + tạo mới)
            slotTemplateService.generateDailyTemplates(cfg.getConfigId(), now, now.plusDays(1));
        }
    }
}
