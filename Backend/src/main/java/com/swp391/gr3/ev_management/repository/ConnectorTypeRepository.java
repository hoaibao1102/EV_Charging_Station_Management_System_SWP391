package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // ✅ Đánh dấu đây là Repository của Spring (tầng truy cập dữ liệu)
public interface ConnectorTypeRepository extends JpaRepository<ConnectorType, Long> {
    // ✅ Kế thừa JpaRepository<ConnectorType, Long> => có sẵn các hàm CRUD cơ bản:
    // findAll(), findById(), save(), deleteById(), existsById(), v.v.
    // Entity: ConnectorType, khóa chính kiểu Long.

    /**
     * ✅ Kiểm tra xem một loại đầu nối (ConnectorType) có tồn tại hay chưa
     *    dựa vào cặp (code, mode).
     *
     * Trong bảng connector_type có thể có nhiều loại đầu nối (ví dụ: "TYPE2-AC", "CHAdeMO-DC"),
     * nên ta cần đảm bảo không trùng code + mode khi tạo mới.
     *
     * Spring Data JPA tự động sinh câu truy vấn:
     * SELECT COUNT(*) > 0 FROM connector_type WHERE code = :code AND mode = :mode;
     *
     * @param code Mã định danh loại đầu nối (ví dụ: "TYPE2")
     * @param mode Kiểu dòng điện ("AC" hoặc "DC")
     * @return true nếu tồn tại loại đầu nối trùng code + mode, false nếu chưa có
     */
    boolean existsByCodeAndMode(String code, String mode);


    /**
     * ✅ Tìm một ConnectorType theo mã code duy nhất.
     *
     * Dùng khi muốn truy vấn loại đầu nối cụ thể (ví dụ: khi cấu hình trạm sạc hoặc xe).
     *
     * Spring Data JPA sẽ tự động tạo truy vấn tương ứng:
     * SELECT * FROM connector_type WHERE code = :code;
     *
     * @param code Mã loại đầu nối (ví dụ: "CHAdeMO", "TYPE2")
     * @return Đối tượng ConnectorType tương ứng (nếu có)
     */
    ConnectorType findByCode(String code);


    /**
     * ✅ Lấy danh sách các loại đầu nối (ConnectorType) có trong một trạm sạc cụ thể.
     *
     * Truy vấn này đi qua các mối quan hệ:
     * ConnectorType -> ChargingPoints -> Station -> stationId
     *
     * => Tức là: lấy tất cả ConnectorType có ít nhất 1 ChargingPoint thuộc trạm có ID cho trước.
     *
     * Từ khóa DISTINCT đảm bảo không trùng loại đầu nối (mỗi loại chỉ xuất hiện một lần).
     *
     * SQL tương đương:
     * SELECT DISTINCT ct.*
     * FROM connector_type ct
     * JOIN charging_point cp ON cp.connector_type_id = ct.id
     * JOIN charging_station st ON cp.station_id = st.station_id
     * WHERE st.station_id = :stationId;
     *
     * @param stationId ID của trạm sạc
     * @return Danh sách loại đầu nối có tại trạm đó
     */
    List<ConnectorType> findDistinctByChargingPoints_Station_StationId(Long stationId);
}
