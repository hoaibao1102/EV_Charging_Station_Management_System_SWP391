package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // ✅ Đánh dấu đây là Repository của Spring (tầng truy cập dữ liệu)
public interface AdminRepository extends JpaRepository<Admin, Long> {
    // ✅ Kế thừa JpaRepository giúp có sẵn các hàm CRUD như:
    // findAll(), findById(), save(), deleteById(), existsById(), v.v.
    // Tham số <Admin, Long> có nghĩa là Entity là Admin và khóa chính (ID) là kiểu Long.

    /**
     * ✅ Kiểm tra xem một Admin có tồn tại dựa trên userId của User liên kết hay không.
     *
     * Vì bảng `Admin` có quan hệ với bảng `User` (OneToOne hoặc ManyToOne),
     * nên có thể truy cập user thông qua `Admin.user.userId`.
     *
     * Spring Data JPA tự động phân tích tên hàm để tạo truy vấn SQL:
     * → SELECT COUNT(*) > 0 FROM admin WHERE user_id = ?;
     */
    boolean existsByUser_UserId(Long userUserId);

    /**
     * ✅ Truy vấn một Admin theo userId, đồng thời lấy luôn thông tin User liên quan.
     *
     * - `@Query`: cho phép viết truy vấn JPQL (Java Persistence Query Language) thủ công.
     * - `join fetch a.user`: giúp lấy dữ liệu `User` đi kèm (tránh LazyLoadingException).
     *
     * JPQL này tương đương SQL:
     * SELECT *
     * FROM admin a
     * JOIN user u ON a.user_id = u.user_id
     * WHERE u.user_id = :userId;
     *
     * Trả về Optional<Admin> vì có thể không tìm thấy admin tương ứng.
     */
    @Query("select a from Admin a join fetch a.user u where u.userId = :userId")
    Optional<Admin> findByUserIdWithUser(Long userId);
}
