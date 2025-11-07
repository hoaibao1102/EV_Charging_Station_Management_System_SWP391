package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverViolation;
import com.swp391.gr3.ev_management.enums.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // ✅ Đánh dấu đây là Repository (tầng truy cập dữ liệu - Data Access Layer)
public interface DriverViolationRepository extends JpaRepository<DriverViolation, Long> {
    // ✅ Kế thừa JpaRepository => có sẵn CRUD cơ bản (findAll, save, delete, findById,...)
    // Entity: DriverViolation, khóa chính: Long


    /**
     * ✅ Lấy danh sách vi phạm của một driver dựa theo driverId.
     *
     * Spring Data JPA tự động sinh truy vấn SQL:
     * SELECT * FROM driver_violation WHERE driver_id = :driverId;
     *
     * @param driverId ID của driver
     * @return Danh sách các vi phạm của driver tương ứng
     */
    List<DriverViolation> findByDriver_DriverId(Long driverId);


    /**
     * ✅ Đếm số lượng vi phạm của một user dựa trên userId và trạng thái (status).
     *
     * Dùng để xác định ví dụ như driver có bao nhiêu vi phạm đang ACTIVE (chưa xử lý).
     *
     * JPQL được viết thủ công:
     * SELECT COUNT(dv)
     * FROM DriverViolation dv
     * JOIN dv.driver d
     * WHERE d.user.userId = :userId
     *   AND dv.status = :status;
     *
     * @param userId  ID của user (liên kết qua driver)
     * @param status  Trạng thái vi phạm (ACTIVE, PAID, CANCELLED, ...)
     * @return Số lượng vi phạm tương ứng
     */
    @Query("""
        SELECT COUNT(dv) FROM DriverViolation dv 
        JOIN dv.driver d 
        WHERE d.user.userId = :userId 
        AND dv.status = :status
    """)
    int countByUserIdAndStatus(@Param("userId") Long userId,
                               @Param("status") ViolationStatus status);


    /**
     * ✅ Lấy danh sách các vi phạm của user dựa theo userId và trạng thái.
     *
     * Dùng JOIN FETCH để lấy luôn dữ liệu của driver và user liên quan trong cùng truy vấn
     * (tránh lỗi LazyInitializationException).
     *
     * JPQL:
     * SELECT dv
     * FROM DriverViolation dv
     * JOIN FETCH dv.driver d
     * JOIN FETCH d.user u
     * WHERE u.userId = :userId
     *   AND dv.status = :status;
     *
     * @param userId  ID của user (người lái xe)
     * @param status  Trạng thái của vi phạm
     * @return Danh sách vi phạm có trạng thái tương ứng
     */
    @Query("""
        SELECT dv FROM DriverViolation dv 
        JOIN FETCH dv.driver d 
        JOIN FETCH d.user u 
        WHERE u.userId = :userId 
        AND dv.status = :status
    """)
    List<DriverViolation> findByUserIdAndStatus(@Param("userId") Long userId,
                                                @Param("status") ViolationStatus status);


    /**
     * ✅ Hàm backup: đếm số lượng vi phạm dựa trên driverId và trạng thái.
     *
     * Giống countByUserIdAndStatus, nhưng truy vấn qua driverId thay vì userId.
     *
     * SQL tương đương:
     * SELECT COUNT(*) FROM driver_violation
     * WHERE driver_id = :driverId AND status = :status;
     *
     * @param driverId ID của driver
     * @param status   Trạng thái của vi phạm
     * @return Số lượng vi phạm tương ứng
     */
    int countByDriver_DriverIdAndStatus(Long driverId, ViolationStatus status);


    /**
     * ✅ Lấy danh sách các vi phạm theo driverId và trạng thái.
     *
     * Spring Data JPA tự động sinh truy vấn:
     * SELECT * FROM driver_violation
     * WHERE driver_id = :driverId AND status = :status;
     *
     * @param driverId ID của driver
     * @param status   Trạng thái vi phạm
     * @return Danh sách vi phạm tương ứng
     */
    List<DriverViolation> findByDriver_DriverIdAndStatus(Long driverId, ViolationStatus status);


    /**
     * ✅ Kiểm tra xem một driver có vi phạm nào chứa mô tả cụ thể hay không.
     *
     * Dùng để tránh tạo trùng vi phạm (ví dụ: "Quá thời gian đặt chỗ" xuất hiện nhiều lần).
     *
     * Spring Data JPA tự động sinh truy vấn:
     * SELECT COUNT(*) > 0 FROM driver_violation
     * WHERE driver_id = :driverId AND description LIKE %:description%;
     *
     * @param driverId ID của driver
     * @param description Nội dung mô tả (một phần hoặc toàn bộ)
     * @return true nếu có vi phạm trùng mô tả, false nếu không có
     */
    boolean existsByDriver_DriverIdAndDescriptionContaining(Long driverId, String description);

}
