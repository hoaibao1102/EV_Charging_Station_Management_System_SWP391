package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // ✅ Đánh dấu đây là một Repository trong Spring, nơi làm việc với database (tầng DAO)
public interface ChargingPointRepository extends JpaRepository<ChargingPoint,Long> {
    // ✅ Kế thừa JpaRepository<ChargingPoint, Long> giúp có sẵn các hàm CRUD như:
    // findAll(), findById(), save(), deleteById(), existsById(), v.v.
    // Trong đó entity là ChargingPoint và khóa chính có kiểu Long.

    /**
     * ✅ Lấy danh sách tất cả các ChargingPoint thuộc về một trạm sạc cụ thể (stationId).
     *
     * Spring Data JPA sẽ tự động phân tích tên hàm để tạo truy vấn tương ứng:
     * SELECT * FROM charging_point WHERE station_id = :stationId;
     *
     * @param stationId ID của trạm sạc (station)
     * @return Danh sách tất cả điểm sạc (ChargingPoint) thuộc trạm đó
     */
    List<ChargingPoint> findByStation_StationId(Long stationId);


    /**
     * ✅ Tìm một ChargingPoint dựa trên serial number (số sê-ri duy nhất).
     *
     * Dùng trong trường hợp cần kiểm tra tính duy nhất của điểm sạc hoặc tìm kiếm thiết bị cụ thể.
     * Spring Data JPA sẽ tạo truy vấn:
     * SELECT * FROM charging_point WHERE serial_number = :serialNumber;
     *
     * @param serialNumber Mã sê-ri của điểm sạc
     * @return Optional chứa ChargingPoint (nếu tìm thấy), rỗng nếu không có
     */
    Optional<ChargingPoint> findBySerialNumber(String serialNumber);

    /**
     * ✅ Tìm một điểm sạc dựa trên "số thứ tự điểm sạc" (pointNumber) và ID của trạm sạc (stationId).
     *
     * Thường dùng khi mỗi trạm có nhiều điểm sạc (Point 1, Point 2, ...),
     * và cần xác định duy nhất bằng cặp (stationId + pointNumber).
     *
     * SQL tương đương:
     * SELECT * FROM charging_point WHERE station_id = :stationId AND point_number = :pointNumber;
     *
     * @param stationId ID trạm sạc
     * @param pointNumber Số hiệu điểm sạc (ví dụ: "P1", "P2", "P3")
     * @return Optional chứa ChargingPoint nếu tìm thấy
     */
    Optional<ChargingPoint> findByStation_StationIdAndPointNumber(Long stationId, String pointNumber);

    /**
     * ✅ Lấy danh sách các điểm sạc của một trạm theo loại đầu nối (connector type).
     *
     * Giúp lọc các điểm sạc trong trạm theo loại đầu nối tương thích với xe điện.
     *
     * SQL tương đương:
     * SELECT * FROM charging_point
     * WHERE station_id = :stationId
     *   AND connector_type_id = :connectorTypeId;
     *
     * @param station_stationId ID trạm sạc
     * @param connectorType_connectorTypeId ID loại đầu nối
     * @return Danh sách điểm sạc (ChargingPoint) phù hợp
     */
    List<ChargingPoint> findByStation_StationIdAndConnectorType_ConnectorTypeId(
            Long station_stationId, Long connectorType_connectorTypeId
    );
}
