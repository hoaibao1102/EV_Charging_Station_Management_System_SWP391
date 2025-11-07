package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle,Long> {

    /**
     * ✅ Lấy danh sách phương tiện (xe) của một tài xế, đồng thời fetch (tải trước)
     *    các thông tin chi tiết liên quan như model (dòng xe) và loại cổng sạc (connector type).
     *
     * 👉 Ý nghĩa:
     * - Một driver có thể sở hữu nhiều xe (UserVehicle).
     * - Mỗi xe có thông tin model (VehicleModel), và mỗi model gắn với loại cổng sạc (ConnectorType).
     * - Query này giúp lấy **tất cả thông tin liên quan trong một lần truy vấn**, tránh lỗi lazy loading.
     *
     * ⚙️ JPQL Query:
     * SELECT v
     * FROM UserVehicle v
     *   LEFT JOIN FETCH v.model m
     *   LEFT JOIN FETCH m.connectorType
     * WHERE v.driver.driverId = :driverId
     *
     * 💡 Giải thích:
     * - `LEFT JOIN FETCH v.model m`: tải luôn thông tin model của xe.
     * - `LEFT JOIN FETCH m.connectorType`: tải luôn loại cổng sạc (ví dụ: CCS2, Type2, CHAdeMO...).
     * - `WHERE v.driver.driverId = :driverId`: lọc theo tài xế cụ thể.
     *
     * 🧩 Dùng trong:
     * - API "Driver xem danh sách xe của mình" (`/api/driver/vehicles`)
     * - Khi hiển thị danh sách xe có thông tin chi tiết về loại sạc tương ứng.
     *
     * @param driverId ID của tài xế (Driver)
     * @return Danh sách các xe của tài xế, kèm thông tin chi tiết model & connector
     */
    @Query("SELECT v FROM UserVehicle v " +
            "LEFT JOIN FETCH v.model m " +
            "LEFT JOIN FETCH m.connectorType " +
            "WHERE v.driver.driverId = :driverId")
    List<UserVehicle> findByDriverIdWithDetails(@Param("driverId") Long driverId);
}
