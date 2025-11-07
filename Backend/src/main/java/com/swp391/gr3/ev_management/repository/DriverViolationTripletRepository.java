package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.DriverViolationTriplet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverViolationTripletRepository extends JpaRepository<DriverViolationTriplet, Long> {
    // ✅ Repository này kế thừa JpaRepository => có sẵn các CRUD cơ bản (findAll, save, delete, findById, ...)
    // ✅ Chuyên dùng để truy vấn bảng DriverViolationTriplet (lưu bộ 3 vi phạm của 1 tài xế để xử lý việc ban tự động)

    /**
     * ✅ Lấy danh sách các "triplet" (bộ 3 vi phạm) đang mở (IN_PROGRESS) của một tài xế.
     *
     * 👉 Ý nghĩa:
     * - Khi 1 tài xế có nhiều vi phạm, hệ thống có thể nhóm lại thành bộ 3 (triplet) để kiểm soát việc tạm khóa tài khoản.
     * - Truy vấn này dùng để lấy tất cả các bộ triplet chưa hoàn tất (trạng thái IN_PROGRESS) của 1 driver cụ thể.
     *
     * 🔍 JPQL:
     * SELECT t FROM DriverViolationTriplet t
     * WHERE t.driver.driverId = :driverId
     *   AND t.status = 'IN_PROGRESS'
     * ORDER BY t.createdAt DESC
     *
     * @param driverId ID của tài xế
     * @return Danh sách các triplet đang mở (chưa kết thúc)
     */
    @Query("""
        select t from DriverViolationTriplet t
        where t.driver.driverId = :driverId and t.status = 'IN_PROGRESS'
        order by t.createdAt desc
    """)
    List<DriverViolationTriplet> findOpenByDriver(@Param("driverId") Long driverId);


    /**
     * ✅ Kiểm tra xem một vi phạm (violationId) có nằm trong bất kỳ triplet nào hay chưa.
     *
     * 👉 Ý nghĩa:
     * - Dùng để tránh gán trùng vi phạm vào nhiều bộ triplet khác nhau.
     * - Một vi phạm chỉ nên nằm trong một triplet tại 1 thời điểm.
     *
     * 🔍 JPQL:
     * SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
     * FROM DriverViolationTriplet t
     * WHERE t.v1.violationId = :violationId
     *    OR t.v2.violationId = :violationId
     *    OR t.v3.violationId = :violationId
     *
     * @param violationId ID của vi phạm cần kiểm tra
     * @return true nếu vi phạm đã thuộc về một triplet, false nếu chưa
     */
    @Query("""
        select case when count(t) > 0 then true else false end
        from DriverViolationTriplet t
        where t.v1.violationId = :violationId
           or t.v2.violationId = :violationId
           or t.v3.violationId = :violationId
    """)
    boolean existsByViolation(@Param("violationId") Long violationId);


    /**
     * ✅ Lấy tất cả các triplet, kèm theo thông tin driver và user (đã JOIN FETCH).
     *
     * 👉 Ý nghĩa:
     * - Khi cần hiển thị danh sách triplet đầy đủ (gồm thông tin tài xế và người dùng),
     *   thay vì chỉ trả về triplet ID, hàm này sẽ join và lấy sẵn dữ liệu liên quan.
     * - Tránh lỗi LazyInitializationException do truy cập ngoài phạm vi session.
     *
     * 🔍 JPQL:
     * SELECT t
     * FROM DriverViolationTriplet t
     *   JOIN FETCH t.driver d
     *   JOIN FETCH d.user u
     * ORDER BY t.createdAt DESC
     *
     * @return Danh sách triplet kèm thông tin driver + user, sắp xếp theo thời gian tạo (mới nhất trước)
     */
    @Query("""
           SELECT t
           FROM DriverViolationTriplet t
             JOIN FETCH t.driver d
             JOIN FETCH d.user u
           ORDER BY t.createdAt DESC
           """)
    List<DriverViolationTriplet> findAllWithDriverAndUser();


    /**
     * ✅ Lấy các triplet (bộ 3 vi phạm) theo số điện thoại người dùng.
     *
     * 👉 Ý nghĩa:
     * - Dùng trong trường hợp admin/staff muốn tra cứu lịch sử vi phạm của tài xế
     *   dựa trên số điện thoại của người dùng (user.phoneNumber).
     *
     * 🔍 JPQL:
     * SELECT t
     * FROM DriverViolationTriplet t
     *   JOIN FETCH t.driver d
     *   JOIN FETCH d.user u
     * WHERE u.phoneNumber = :phone
     * ORDER BY t.createdAt DESC
     *
     * @param phone Số điện thoại của user
     * @return Danh sách triplet liên quan đến user có số điện thoại này
     */
    @Query("""
           SELECT t
           FROM DriverViolationTriplet t
             JOIN FETCH t.driver d
             JOIN FETCH d.user u
           WHERE u.phoneNumber = :phone
           ORDER BY t.createdAt DESC
           """)
    List<DriverViolationTriplet> findByUserPhoneNumber(@Param("phone") String phone);
}
