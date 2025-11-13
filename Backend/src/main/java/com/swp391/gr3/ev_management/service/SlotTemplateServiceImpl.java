package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.SlotTemplateResponse;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.entity.SlotTemplate;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.SlotTemplateMapper;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import com.swp391.gr3.ev_management.repository.SlotTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu đây là Spring Service (xử lý logic cho SlotTemplate)
@RequiredArgsConstructor // Lombok tạo constructor cho các field final (DI)
public class SlotTemplateServiceImpl implements SlotTemplateService {

    private final SlotConfigRepository slotConfigRepository;   // Repository làm việc với SlotConfig
    private final SlotTemplateRepository slotTemplateRepository; // Repository làm việc với SlotTemplate
    private final SlotTemplateMapper mapper;                   // Mapper chuyển Entity <-> DTO SlotTemplateResponse

    /**
     * Sinh các SlotTemplate trong 1 ngày (dựa trên SlotConfig).
     *
     * Ý tưởng:
     *  - Lấy cấu hình slot (SlotConfig) theo configId.
     *  - Đọc slotDurationMin (số phút mỗi slot) từ config.
     *  - Chia full 24h của ngày (00:00 -> 24:00) thành các slot liên tiếp.
     *  - Trước khi tạo mới: xóa tất cả template của ngày này (tránh trùng).
     *  - Lưu danh sách template mới và trả về dạng DTO.
     *
     * Tham số:
     *  - configId: id cấu hình slot cho 1 trạm.
     *  - forDate: thời điểm thuộc ngày cần generate (chỉ dùng phần ngày, bỏ phần giờ).
     *  - endDate: hiện tại không dùng trong logic này, nhưng được giữ để tương thích chữ ký method.
     */
    @Override
    @Transactional // Có thao tác ghi (delete + save), cần transaction đảm bảo toàn vẹn
    public List<SlotTemplateResponse> generateDailyTemplates(Long configId, LocalDateTime forDate, LocalDateTime endDate) {
        // 1️⃣ Lấy SlotConfig theo id, nếu không có -> ném lỗi
        SlotConfig config = slotConfigRepository.findByConfigId(configId);
        if (config == null) {
            throw new ErrorException("Không tìm thấy SlotConfig với id = " + configId);
        }

        // 2️⃣ Lấy độ dài mỗi slot (phút) từ config
        int duration = config.getSlotDurationMin();
        if (duration <= 0) {
            // slotDurationMin phải > 0 để chia khung giờ
            throw new ErrorException("slotDurationMin phải > 0.");
        }

        // 3️⃣ Định nghĩa khung giờ cố định cho cả ngày:
        //    windowStart: 00:00 ngày forDate
        //    windowEndExclusive: 00:00 ngày hôm sau (exclusive)
        LocalDateTime windowStart = forDate.toLocalDate().atStartOfDay();           // 00:00
        LocalDateTime windowEndExclusive = windowStart.plusDays(1);                 // 24:00 (00:00 ngày tiếp theo)
        long totalMinutes = Duration.between(windowStart, windowEndExclusive).toMinutes(); // 1440 phút cho 1 ngày

        // 4️⃣ Kiểm tra 24h có chia hết cho duration hay không (để các slot bằng nhau, không bị dư)
        if (totalMinutes % duration != 0) {
            throw new ErrorException(
                    "Khoảng thời gian 24 giờ (" + totalMinutes + " phút) không chia hết cho slotDurationMin = " + duration
            );
        }

        // 5️⃣ XÓA CÁC TEMPLATE TRONG NGÀY NÀY
        //    - Dùng khoảng [windowStart, windowEndExclusive - 1ns] để không chạm sang 00:00 ngày kế tiếp
        slotTemplateRepository.deleteByConfig_ConfigIdAndStartTimeBetween(
                configId,
                windowStart,
                windowEndExclusive.minusNanos(1)
        );

        // 6️⃣ Tính số slot trong cả ngày (vd: 1440 / 30 = 48 slot)
        int slots = (int) (totalMinutes / duration);
        List<SlotTemplate> toSave = new ArrayList<>(slots); // pre-allocate size cho hiệu năng tốt hơn

        // 7️⃣ Tạo lần lượt các SlotTemplate theo index
        for (int i = 0; i < slots; i++) {
            // 7.1) start = 00:00 + i * duration
            LocalDateTime start = windowStart.plusMinutes((long) i * duration);
            // 7.2) end = start + duration (đảm bảo không vượt quá windowEndExclusive nhờ kiểm tra chia hết phía trên)
            LocalDateTime end = start.plusMinutes(duration);

            // 7.3) Build entity SlotTemplate
            SlotTemplate template = SlotTemplate.builder()
                    .slotIndex(i + 1) // đánh số thứ tự slot (bắt đầu từ 1)
                    .startTime(start) // thời gian bắt đầu slot
                    .endTime(end)     // thời gian kết thúc slot
                    .config(config)   // gắn với SlotConfig hiện tại
                    .build();

            // 7.4) Thêm vào danh sách chờ save
            toSave.add(template);
        }

        // 8️⃣ Lưu toàn bộ danh sách SlotTemplate xuống DB và map sang DTO trả về
        return slotTemplateRepository.saveAll(toSave)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy 1 SlotTemplate theo id và map sang DTO.
     * Nếu không tồn tại -> mapper sẽ xử lý null (theo implement bên trong).
     */
    @Override
    public SlotTemplateResponse getById(Long templateId) {
        return mapper.toResponse(slotTemplateRepository.findById(templateId).orElse(null));
    }

    /**
     * Lấy tất cả SlotTemplate trong hệ thống (dùng cho admin/debug).
     */
    @Override
    public List<SlotTemplateResponse> getAll() {
        return slotTemplateRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách SlotTemplate thuộc 1 SlotConfig trong khoảng startTime [start, end).
     * Dùng cho các logic generate availability / filter theo ngày.
     */
    @Override
    public List<SlotTemplate> findByConfig_ConfigIdAndStartTimeBetween(Long configId, LocalDateTime start, LocalDateTime end) {
        return slotTemplateRepository.findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);
    }

    /**
     * Lấy danh sách SlotTemplate theo danh sách id (batch lookup).
     */
    @Override
    public List<SlotTemplate> findAllById(List<Long> templateIds) {
        return slotTemplateRepository.findAllById(templateIds);
    }
}
