package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.SlotAvailability;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service // Đánh dấu đây là một Spring Service (xử lý logic sinh SlotAvailability theo lịch)
@RequiredArgsConstructor // Lombok tạo constructor cho các field final (DI)
public class SlotAvailabilitySchedulerService {

    // ====== Các service phụ thuộc để thao tác dữ liệu liên quan đến Slot ======
    private final SlotConfigService slotConfigService;           // Lấy thông tin cấu hình slot (SlotConfig)
    private final SlotTemplateService slotTemplateService;       // Lấy danh sách SlotTemplate theo config & thời gian
    private final SlotAvailabilityService slotAvailabilityService; // CRUD SlotAvailability (khoảng thời gian/point bookable)
    private final ChargingPointService chargingPointService;     // Lấy danh sách ChargingPoint theo station

    /**
     * Reset toàn bộ SlotAvailability của 1 config trong NGÀY (xóa rồi tạo lại theo templates & points hiện có).
     *
     * Flow:
     *  1. Kiểm tra SlotConfig tồn tại.
     *  2. Xác định khoảng thời gian của ngày [00:00, 24:00) theo LocalDate truyền vào.
     *  3. Xóa tất cả SlotAvailability trong ngày tương ứng với configId.
     *  4. Lấy danh sách SlotTemplate trong ngày theo configId.
     *  5. Lấy tất cả ChargingPoint thuộc station của config.
     *  6. Sinh SlotAvailability mới = mọi kết hợp (template x point) với trạng thái AVAILABLE.
     *  7. Lưu hàng loạt và trả về số record đã tạo.
     *
     * YÊU CẦU: đã có templates cho ngày đó.
     */
    @Transactional // Đảm bảo toàn bộ thao tác (xóa + tạo) diễn ra trong một transaction
    public int resetAndCreateForConfigInDate(Long configId, LocalDate date) {
        // 1️⃣ Lấy SlotConfig từ DB theo configId
        var config = slotConfigService.findEntityById(configId);
        if (config == null) {
            // Không tìm thấy config -> ném lỗi nghiệp vụ
            throw new ErrorException("Không tìm thấy SlotConfig id=" + configId);
        }

        // 2️⃣ Xác định khoảng thời gian trong NGÀY tương ứng với LocalDate truyền vào
        //    - start: 00:00:00 của ngày đó
        //    - end:   00:00:00 của ngày kế tiếp (để dùng trong điều kiện BETWEEN)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        // 3️⃣ XÓA TẤT CẢ SlotAvailability thuộc configId trong khoảng [start, end)
        //    Lưu ý: phải xóa Availability trước nếu muốn regenerate lại trong cùng ngày,
        //           tránh bị trùng hoặc giữ lại dữ liệu cũ.
        slotAvailabilityService.deleteByTemplate_Config_ConfigIdAndDateBetween(configId, start, end);

        // 4️⃣ LẤY DANH SÁCH SlotTemplate TRONG NGÀY (theo configId & startTime nằm trong [start, end))
        var templates = slotTemplateService.findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);
        if (templates == null || templates.isEmpty()) {
            // Nếu chưa có template cho ngày này thì không thể tạo availability → trả 0
            return 0;
        }

        // 5️⃣ LẤY TẤT CẢ CHARGING POINT thuộc station tương ứng với SlotConfig
        //    - Mỗi SlotAvailability đại diện cho (template, chargingPoint)
        Long stationId = config.getStation().getStationId();
        List<com.swp391.gr3.ev_management.entity.ChargingPoint> points =
                chargingPointService.findByStation_StationId(stationId);

        if (points == null || points.isEmpty()) {
            // Không có điểm sạc nào -> không tạo được Availability
            return 0;
        }

        // 6️⃣ TẠO DANH SÁCH SlotAvailability MỚI (CARTESIAN: MỖI TEMPLATE X MỖI POINT)
        //    - Khởi tạo ArrayList với capacity = templates.size() * points.size() để giảm resize
        var toSave = new ArrayList<SlotAvailability>(templates.size() * points.size());
        for (var t : templates) {
            // Xác định ngày (đã chuẩn hóa về 00:00:00) từ startTime của template
            LocalDateTime day = t.getStartTime().withHour(0).withMinute(0).withSecond(0).withNano(0);
            for (var p : points) {
                // Với mỗi cặp (template, point) -> tạo 1 SlotAvailability AVAILABLE
                toSave.add(SlotAvailability.builder()
                        .template(t)              // template (khung giờ) được áp dụng
                        .chargingPoint(p)        // điểm sạc tương ứng
                        .status(SlotStatus.AVAILABLE) // trạng thái ban đầu: AVAILABLE (sẵn sàng để đặt)
                        .date(day)               // ngày áp dụng (chung cho cả ngày)
                        .build());
            }
        }

        // 7️⃣ LƯU TOÀN BỘ SlotAvailability vào DB bằng batch save
        //    - saveAll(toSave) trả về list đã lưu; lấy size() làm số lượng record mới tạo
        return slotAvailabilityService.saveAll(toSave).size();
    }
}
