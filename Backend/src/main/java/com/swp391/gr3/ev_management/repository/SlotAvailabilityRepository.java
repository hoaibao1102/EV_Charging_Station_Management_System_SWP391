package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotAvailabilityRepository extends JpaRepository<SlotAvailability, Long> {
    // ✅ Repository này quản lý entity "SlotAvailability" — dùng để lưu thông tin về trạng thái khả dụng (availability)
    //    của các khung giờ sạc (slot) tại các điểm sạc (charging point).
    // ✅ Kế thừa JpaRepository => có sẵn các phương thức CRUD cơ bản (findAll, save, deleteById, ...)

    /**
     * ✅ Kiểm tra xem slot availability (khung giờ sạc khả dụng) đã tồn tại hay chưa
     *    cho một template, charging point và ngày cụ thể.
     *
     * 👉 Ý nghĩa:
     * - Dùng khi tạo mới slot availability để tránh bị trùng.
     * - Ví dụ: không tạo 2 bản ghi cho cùng `templateId`, `pointId`, và `date`.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT COUNT(*) > 0
     * FROM slot_availability
     * WHERE template_id = :templateId
     *   AND charging_point_id = :pointId
     *   AND date = :date
     *
     * 💡 Trả về `true` nếu slot đã tồn tại, `false` nếu chưa có.
     *
     * @param templateId ID của slot template
     * @param pointId ID của điểm sạc
     * @param date Ngày cụ thể của slot
     * @return boolean — có tồn tại slot đó hay không
     */
    boolean existsByTemplate_TemplateIdAndChargingPoint_PointIdAndDate(Long templateId, Long pointId, LocalDateTime date);


    /**
     * ✅ Xóa tất cả các slot availability theo cấu hình (configId) trong khoảng thời gian chỉ định.
     *
     * 👉 Ý nghĩa:
     * - Khi admin thay đổi cấu hình slot (slot config) hoặc muốn làm mới các slot,
     *   hệ thống cần xóa tất cả các slot availability trong một khoảng ngày cụ thể.
     *
     * ⚙️ Query tự động sinh ra:
     * DELETE FROM slot_availability
     * WHERE config_id = :configId
     *   AND date BETWEEN :start AND :end
     *
     * 💡 Hữu ích khi cần "tái tạo" slot availability (ví dụ: khi admin đổi giờ hoạt động của trạm sạc).
     *
     * @param configId ID của slot config
     * @param start thời gian bắt đầu khoảng cần xóa
     * @param end thời gian kết thúc khoảng cần xóa
     */
    void deleteByTemplate_Config_ConfigIdAndDateBetween(Long configId, LocalDateTime start, LocalDateTime end);


    /**
     * ✅ Lấy tất cả các slot availability của một điểm sạc cụ thể.
     *
     * 👉 Ý nghĩa:
     * - Dùng để hiển thị danh sách các slot khả dụng cho người dùng chọn khi đặt lịch sạc.
     * - Ví dụ: Lấy tất cả slot availability của pointId = 5 (tức trạm sạc số 5).
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM slot_availability
     * WHERE charging_point_id = :pointId
     *
     * 💡 Có thể dùng để hiển thị lịch sạc (availability calendar) của từng điểm sạc.
     *
     * @param pointId ID của điểm sạc (charging point)
     * @return danh sách các slot availability thuộc điểm sạc đó
     */
    List<SlotAvailability> findAllByChargingPoint_PointId(Long pointId);
}
