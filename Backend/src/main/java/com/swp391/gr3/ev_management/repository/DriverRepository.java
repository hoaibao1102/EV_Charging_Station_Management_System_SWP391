package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// ✅ Repository quản lý entity Driver, cho phép thao tác CRUD và custom query
public interface DriverRepository extends JpaRepository<Driver, Long> {

    /**
     * ✅ Kiểm tra xem một Driver có tồn tại hay không dựa trên userId của user liên kết.
     *
     * Entity Driver có quan hệ 1-1 hoặc nhiều-1 với entity User → do đó có thể truy cập user.userId.
     *
     * Spring Data JPA sẽ tự sinh truy vấn tương ứng:
     * SELECT COUNT(*) > 0 FROM driver WHERE user_id = :userId;
     *
     * @param userId ID của User cần kiểm tra
     * @return true nếu driver có tồn tại, false nếu không
     */
    boolean existsByUser_UserId(Long userId);

    /**
     * ✅ Lấy thông tin Driver theo userId và đồng thời JOIN FETCH với entity User.
     *
     * Dùng khi bạn cần chắc chắn dữ liệu của User được nạp sẵn (tránh LazyInitializationException).
     *
     * JPQL:
     * SELECT d FROM Driver d
     * JOIN FETCH d.user u
     * WHERE u.userId = :userId;
     *
     * => Trả về Optional<Driver> (rỗng nếu không tìm thấy)
     *
     * @param userId ID của User liên kết
     * @return Optional chứa Driver + User đã load đầy đủ
     */
    @Query("""
           select d from Driver d 
           join fetch d.user u 
           where u.userId = :userId
           """)
    Optional<Driver> findByUserIdWithUser(@Param("userId") Long userId);

    /**
     * ✅ Tương tự như trên nhưng truy vấn theo driverId (khóa chính của Driver).
     *
     * JOIN FETCH để lấy luôn dữ liệu User liên quan trong cùng một truy vấn.
     *
     * JPQL:
     * SELECT d FROM Driver d
     * JOIN FETCH d.user u
     * WHERE d.driverId = :driverId;
     *
     * @param driverId ID của Driver
     * @return Optional chứa Driver + User
     */
    @Query("""
           select d from Driver d 
           join fetch d.user u 
           where d.driverId = :driverId
           """)
    Optional<Driver> findByDriverIdWithUser(@Param("driverId") Long driverId);

    /**
     * ✅ Tìm Driver theo userId (dùng khi không cần JOIN FETCH thủ công).
     *
     * Spring Data JPA sẽ tự động sinh SQL:
     * SELECT * FROM driver WHERE user_id = :userId;
     *
     * @param userId ID của user
     * @return Optional chứa Driver nếu tồn tại
     */
    Optional<Driver> findByUser_UserId(Long userId);

    /**
     * ✅ Đếm số lượng driver theo trạng thái (ACTIVE, SUSPENDED, BANNED, ...)
     *
     * SQL tương đương:
     * SELECT COUNT(*) FROM driver WHERE status = :status;
     *
     * Dùng để thống kê trong dashboard hoặc báo cáo quản trị.
     *
     * @param status Trạng thái của driver
     * @return Số lượng driver có status tương ứng
     */
    long countByStatus(DriverStatus status);

    @Query("""
    select d
    from Driver d
    join fetch d.user u
    where u.userId = :userId
    """)
    Optional<Driver> findByUserIdLight(Long userId);
}
