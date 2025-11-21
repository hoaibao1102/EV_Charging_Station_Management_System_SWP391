package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff,Long> {
    // ✅ Repository này quản lý entity "Tariff" — đại diện cho bảng giá (biểu phí sạc điện)
    //    của từng loại cổng sạc (ConnectorType), có thời gian hiệu lực (effectiveFrom - effectiveTo).

    /**
     * ✅ Tìm Tariff (biểu phí) theo loại đầu nối (ConnectorType).
     *
     * 👉 Ý nghĩa:
     * - Mỗi loại cổng sạc (ConnectorType) có thể có một hoặc nhiều mức giá khác nhau.
     * - Hàm này dùng để lấy biểu phí gắn với một loại cổng sạc cụ thể.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM tariff WHERE connector_type_id = :connectorType LIMIT 1;
     *
     * 💡 Trả về `Optional<Tariff>` vì có thể không tồn tại biểu phí nào cho loại cổng đó.
     *
     * @param connectorType entity ConnectorType (cổng sạc)
     * @return Optional chứa biểu phí nếu có
     */
    Optional<Tariff> findByConnectorType(ConnectorType connectorType);


    /**
     * ✅ Tìm **biểu phí đang hoạt động (active)** cho một loại cổng sạc tại thời điểm cụ thể.
     *
     * 👉 Ý nghĩa:
     * - Mỗi biểu phí có thời gian hiệu lực `effectiveFrom` và `effectiveTo`.
     * - Phương thức này tìm biểu phí mà thời gian hiện tại (hoặc thời điểm chỉ định)
     *   nằm trong khoảng thời gian hiệu lực đó.
     * - Nếu có nhiều bản ghi hợp lệ, nó sẽ lấy **bản có ngày bắt đầu mới nhất (gần hiện tại nhất)**.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM tariff
     * WHERE connector_type_id = :connectorTypeId
     *   AND effective_from <= :from
     *   AND effective_to >= :to
     * ORDER BY effective_from DESC
     * LIMIT 1;
     *
     * 💡 Dùng khi bạn cần xác định biểu phí hiện hành của một đầu sạc tại thời điểm tính toán.
     *
     * @param connectorTypeId ID của loại đầu nối (ConnectorType)
     * @param from thời điểm hiện tại (hoặc bắt đầu kiểm tra)
     * @param to thời điểm hiện tại (hoặc kết thúc kiểm tra)
     * @return Optional chứa biểu phí hợp lệ
     */
    Optional<Tariff> findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
            Long connectorTypeId, LocalDateTime from, LocalDateTime to
    );


    /**
     * ✅ Lấy danh sách tất cả biểu phí **đang có hiệu lực** cho một loại đầu nối tại thời điểm `now`.
     *
     * 👉 Ý nghĩa:
     * - Lọc tất cả các Tariff mà `now` nằm trong khoảng hiệu lực (effectiveFrom → effectiveTo).
     * - Kết quả sắp xếp theo ngày bắt đầu (effectiveFrom) giảm dần — ưu tiên bản mới nhất ở đầu danh sách.
     *
     * ⚙️ JPQL Query:
     * SELECT t FROM Tariff t
     * WHERE t.connectorType.connectorTypeId = :connectorTypeId
     *   AND :now BETWEEN t.effectiveFrom AND t.effectiveTo
     * ORDER BY t.effectiveFrom DESC;
     *
     * 💡 Dùng để hiển thị tất cả các mức giá hiện hành cho một loại cổng sạc.
     *
     * @param connectorTypeId ID của loại đầu nối
     * @param now thời điểm hiện tại
     * @return danh sách các biểu phí đang hoạt động
     */
    @Query("""
           SELECT t FROM Tariff t
           WHERE t.connectorType.connectorTypeId = :connectorTypeId
             AND :now BETWEEN t.effectiveFrom AND t.effectiveTo
           ORDER BY t.effectiveFrom DESC
           """)
    List<Tariff> findActiveByConnectorType(@Param("connectorTypeId") Long connectorTypeId,
                                           @Param("now") LocalDateTime now);

    @Query(value = """
    SELECT TOP 1 t.price_per_min
    FROM tariffs t           -- ✅ đúng tên bảng trong DB (snake_case, số nhiều)
    WHERE t.connector_typeid = :connectorId
      AND t.effective_from <= :now
      AND t.effective_to   >= :now
    ORDER BY t.effective_from DESC
    """,
            nativeQuery = true)
    Optional<Double> findPricePerMinActive(
            @Param("connectorId") Long connectorId,
            @Param("now") LocalDateTime now
    );
}
