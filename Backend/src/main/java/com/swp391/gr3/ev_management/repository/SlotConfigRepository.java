package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotConfigRepository extends JpaRepository<SlotConfig, Long> {
    // ✅ Repository này quản lý entity "SlotConfig" — đại diện cho cấu hình slot (số lượng slot, thời gian bắt đầu/kết thúc, khoảng cách giữa các slot,...)
    // ✅ JpaRepository giúp có sẵn các hàm CRUD cơ bản (findAll, save, delete, findById, ...)

    /**
     * ✅ Tìm SlotConfig theo ID (khóa chính).
     *
     * 👉 Ý nghĩa:
     * - Dùng để lấy ra một cấu hình slot cụ thể trong hệ thống.
     * - Ví dụ: lấy thông tin config để hiển thị hoặc cập nhật.
     *
     * ⚙️ Query tự động được Spring Data JPA sinh ra:
     * SELECT * FROM slot_config WHERE config_id = :slotConfigId;
     *
     * @param slotConfigId ID của slot config
     * @return SlotConfig tương ứng (nếu tồn tại)
     */
    SlotConfig findByConfigId(Long slotConfigId);


    /**
     * ✅ Tìm SlotConfig theo ID của trạm sạc (stationId).
     *
     * 👉 Ý nghĩa:
     * - Mỗi trạm sạc (Charging Station) có thể có 1 cấu hình slot riêng.
     * - Hàm này dùng để tìm cấu hình slot gắn với trạm sạc cụ thể.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM slot_config WHERE station_id = :stationId;
     *
     * @param stationId ID của trạm sạc
     * @return SlotConfig gắn với trạm đó
     */
    SlotConfig findByStation_StationId(Long stationId);


    /**
     * ✅ Lấy danh sách SlotConfig theo trạng thái hoạt động (ACTIVE / INACTIVE / EXPIRED).
     *
     * 👉 Ý nghĩa:
     * - Dùng khi cần lọc danh sách config đang hoạt động hoặc đã bị vô hiệu.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM slot_config WHERE is_active = :isActive;
     *
     * @param isActive trạng thái cấu hình (ACTIVE, INACTIVE, ...)
     * @return danh sách cấu hình slot theo trạng thái
     */
    List<SlotConfig> findByIsActive(SlotConfigStatus isActive);


    /**
     * ✅ Kiểm tra xem một trạm sạc có cấu hình "ACTIVE" hay không.
     *
     * 👉 Ý nghĩa:
     * - Khi tạo mới cấu hình slot cho trạm, cần kiểm tra xem trạm đó đã có cấu hình ACTIVE chưa,
     *   để tránh việc có nhiều cấu hình hoạt động cùng lúc.
     *
     * ⚙️ Query dùng @Query annotation (viết tay):
     * SELECT COUNT(c) > 0
     * FROM SlotConfig c
     * WHERE c.station.stationId = :stationId
     *   AND c.isActive = :status;
     *
     * 💡 Trả về true nếu có ít nhất 1 config đang ACTIVE, ngược lại false.
     *
     * @param stationId ID của trạm sạc
     * @param status trạng thái cần kiểm tra (thường là ACTIVE)
     * @return true nếu tồn tại cấu hình đang hoạt động
     */
    @Query("SELECT COUNT(c) > 0 FROM SlotConfig c WHERE c.station.stationId = :stationId AND c.isActive = :status")
    boolean existsActiveConfig(@Param("stationId") Long stationId, @Param("status") SlotConfigStatus status);


    /**
     * ✅ Vô hiệu hóa (deactivate) tất cả cấu hình slot "ACTIVE" của một trạm.
     *
     * 👉 Ý nghĩa:
     * - Khi admin kích hoạt cấu hình mới cho trạm sạc, cần tắt (INACTIVE) cấu hình cũ đang hoạt động.
     * - Thực hiện cập nhật hàng loạt (batch update) để tránh lỗi trùng cấu hình.
     *
     * ⚙️ Query viết tay:
     * UPDATE SlotConfig
     * SET is_active = 'INACTIVE',
     *     active_expire = :now
     * WHERE station_id = :stationId
     *   AND is_active = 'ACTIVE';
     *
     * 💡 `@Modifying` dùng để chỉ ra đây là câu lệnh UPDATE chứ không phải SELECT.
     * 💡 `clearAutomatically` & `flushAutomatically` đảm bảo đồng bộ dữ liệu trong context.
     *
     * @param stationId ID của trạm cần vô hiệu hóa cấu hình
     * @param now thời điểm cập nhật trạng thái (thường là thời điểm hiện tại)
     * @return số lượng bản ghi bị ảnh hưởng (số cấu hình bị tắt)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SlotConfig c SET c.isActive = 'INACTIVE', c.activeExpire = :now " +
            "WHERE c.station.stationId = :stationId AND c.isActive = 'ACTIVE'")
    int deactivateActiveByStation(@Param("stationId") Long stationId, @Param("now") LocalDateTime now);
}
