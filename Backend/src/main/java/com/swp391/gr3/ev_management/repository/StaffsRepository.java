package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffsRepository extends JpaRepository<Staffs, Long> {
    // ✅ Repository này quản lý entity "Staffs" — đại diện cho nhân viên (staff) trong hệ thống.
    // ✅ Kế thừa JpaRepository => có sẵn các phương thức CRUD cơ bản (findAll, save, deleteById, ...).

    /**
     * ✅ Tìm nhân viên (Staff) theo userId, đồng thời JOIN FETCH để lấy luôn thông tin User liên quan.
     *
     * 👉 Ý nghĩa:
     * - Mỗi staff có một user liên kết (thông tin đăng nhập, email, tên, v.v...).
     * - JOIN FETCH giúp lấy cả entity `User` cùng lúc, tránh lỗi LazyInitializationException
     *   khi truy cập user sau khi session đóng.
     *
     * ⚙️ JPQL Query:
     * SELECT s
     * FROM Staffs s
     * JOIN FETCH s.user u
     * WHERE u.userId = :userId;
     *
     * 💡 Dùng khi bạn cần truy cập thông tin staff kèm chi tiết user (ví dụ trong profile).
     *
     * @param userId ID của user liên kết với staff
     * @return Optional<Staffs> — có thể rỗng nếu không tìm thấy
     */
    @Query("""
           select s from Staffs s
           join fetch s.user u 
           where u.userId = :userId
           """)
    Optional<Staffs> findByUserIdWithUser(@Param("userId") Long userId);


    /**
     * ✅ Tìm nhân viên (Staff) theo ID của user.
     *
     * 👉 Ý nghĩa:
     * - Cách viết ngắn gọn hơn dùng cú pháp property path của Spring Data JPA.
     * - Trả về đối tượng Staff tương ứng với userId truyền vào.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT * FROM staffs WHERE user_id = :userId;
     *
     * 💡 Không JOIN FETCH — nghĩa là nếu cần dữ liệu `user`, có thể phải fetch thêm (lazy load).
     *
     * @param userId ID của user
     * @return Optional<Staffs> — có thể rỗng nếu không tìm thấy
     */
    Optional<Staffs> findByUser_UserId(Long userId);


    /**
     * ✅ Đếm số lượng staff theo trạng thái (status).
     *
     * 👉 Ý nghĩa:
     * - Dùng để thống kê số lượng nhân viên theo trạng thái (ví dụ: ACTIVE, INACTIVE, SUSPENDED,...).
     * - Hữu ích trong dashboard hoặc báo cáo quản trị.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT COUNT(*) FROM staffs WHERE status = :status;
     *
     * 💡 Trả về số lượng nhân viên có trạng thái tương ứng.
     *
     * @param status trạng thái nhân viên (StaffStatus enum)
     * @return số lượng nhân viên khớp với trạng thái đó
     */
    long countByStatus(StaffStatus status);

    @Query("""
        select s.staffId
        from Staffs s
        where s.user.userId = :userId
    """)
    Optional<Long> findIdByUserId(@Param("userId") Long userId);
}
