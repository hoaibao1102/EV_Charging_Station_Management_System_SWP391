package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotTemplateRepository extends JpaRepository<SlotTemplate, Long> {
    // ✅ Repository này quản lý entity "SlotTemplate" — mô tả mẫu (template) cho các khung giờ sạc (slot)
    //    được sinh ra theo cấu hình slot (SlotConfig).
    // ✅ JpaRepository cung cấp sẵn các hàm CRUD cơ bản (save, findAll, deleteById, ...)

    /**
     * ✅ Lấy danh sách SlotTemplate theo cấu hình (configId) và khoảng thời gian bắt đầu.
     *
     * 👉 Ý nghĩa:
     * - Dùng để lấy tất cả các khung giờ (slot templates) trong một cấu hình cụ thể,
     *   nằm trong một khoảng thời gian xác định.
     * - Ví dụ: lấy các slot trong ngày hôm nay (từ 00:00 đến 23:59).
     *
     * ⚙️ Query tự động sinh ra bởi Spring Data JPA:
     * SELECT * FROM slot_template
     * WHERE config_id = :configId
     *   AND start_time BETWEEN :startInclusive AND :endExclusive;
     *
     * 💡 `startInclusive` và `endExclusive` giúp xác định khoảng thời gian (ví dụ trong 1 ngày hoặc 1 tuần).
     *
     * @param configId ID của cấu hình slot (SlotConfig)
     * @param startInclusive thời gian bắt đầu (bao gồm)
     * @param endExclusive thời gian kết thúc (không bao gồm)
     * @return danh sách SlotTemplate trong khoảng thời gian đó
     */
    List<SlotTemplate> findByConfig_ConfigIdAndStartTimeBetween(
            Long configId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );


    /**
     * ✅ Xóa các SlotTemplate theo cấu hình và khoảng thời gian bắt đầu.
     *
     * 👉 Ý nghĩa:
     * - Dùng khi cần làm mới hoặc cập nhật lại các khung giờ sạc (slot template)
     *   trong một khoảng thời gian cụ thể.
     * - Ví dụ: khi admin thay đổi thời gian hoạt động của trạm sạc,
     *   hệ thống cần xóa các slot cũ để tạo lại.
     *
     * ⚙️ Query tự động sinh ra:
     * DELETE FROM slot_template
     * WHERE config_id = :configId
     *   AND start_time BETWEEN :startInclusive AND :endExclusive;
     *
     * 💡 Đây là một thao tác xóa theo batch — thường được gọi khi cập nhật cấu hình trạm sạc.
     *
     * @param configId ID của cấu hình slot (SlotConfig)
     * @param startInclusive thời gian bắt đầu (bao gồm)
     * @param endExclusive thời gian kết thúc (không bao gồm)
     */
    void deleteByConfig_ConfigIdAndStartTimeBetween(
            Long configId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

}
