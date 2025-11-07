package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // ✅ Đánh dấu đây là một Repository của Spring (tầng truy cập dữ liệu - Data Access Layer)
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
    // ✅ Kế thừa JpaRepository<ChargingStation, Long> để có sẵn các hàm CRUD như:
    // findAll(), findById(), save(), deleteById(), existsById(), v.v.
    // Entity chính là ChargingStation, khóa chính là kiểu Long.


    /**
     * ✅ Tìm một trạm sạc (ChargingStation) theo ID của trạm.
     *
     * Spring Data JPA tự động phân tích tên hàm để tạo truy vấn SQL:
     * SELECT * FROM charging_station WHERE station_id = :id;
     *
     * ⚠️ Lưu ý: Ở đây `findByStationId()` khác với `findById()` mặc định của JpaRepository,
     * vì `stationId` có thể là một trường khác (ví dụ: cột riêng trong entity, không phải khóa chính của DB).
     *
     * @param id ID của trạm sạc
     * @return ChargingStation nếu tìm thấy (hoặc null nếu không)
     */
    ChargingStation findByStationId(long id);


    /**
     * ✅ Kiểm tra xem trạm sạc đã tồn tại hay chưa dựa trên TÊN trạm và ĐỊA CHỈ (không phân biệt hoa thường).
     *
     * Dùng để đảm bảo không có trạm trùng tên và địa chỉ khi tạo mới.
     *
     * Spring Data JPA sẽ tự động sinh truy vấn SQL tương ứng:
     * SELECT COUNT(*) > 0 FROM charging_station
     * WHERE LOWER(station_name) = LOWER(:stationName)
     *   AND LOWER(address) = LOWER(:address);
     *
     * @param stationName Tên trạm sạc
     * @param address     Địa chỉ trạm sạc
     * @return true nếu trạm tồn tại, false nếu chưa có
     */
    boolean existsByStationNameIgnoreCaseAndAddressIgnoreCase(String stationName, String address);


    /**
     * ✅ Tìm trạm sạc theo tên (stationName).
     *
     * Spring Data JPA tự động sinh câu SQL:
     * SELECT * FROM charging_station WHERE station_name = :stationName;
     *
     * Trả về `Optional` để tránh lỗi NullPointerException (nếu không tìm thấy trạm nào).
     *
     * @param stationName Tên trạm sạc
     * @return Optional chứa ChargingStation (nếu có), hoặc Optional.empty() nếu không tìm thấy
     */
    Optional<ChargingStation> findByStationName(String stationName);
}
