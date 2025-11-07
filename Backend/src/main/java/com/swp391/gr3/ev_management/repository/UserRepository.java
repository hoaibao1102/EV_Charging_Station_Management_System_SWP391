package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Tìm người dùng theo số điện thoại và mật khẩu (đã hash)
    // 👉 Dùng trong quá trình đăng nhập, xác thực user.
    // ⚠️ Lưu ý: Trong thực tế, nên xác thực bằng phương thức khác (JWT, bcrypt, ...).
    User findUsersByPhoneNumberAndPasswordHash(String phoneNumber , String password);

    // ✅ Tìm người dùng theo số điện thoại
    // 👉 Thường dùng để kiểm tra xem số điện thoại đã tồn tại trong hệ thống chưa.
    User findUsersByPhoneNumber(String phoneNumber);

    // ✅ Tìm người dùng theo ID (cột userId)
    // 👉 Dùng khi cần lấy thông tin chi tiết của người dùng theo ID.
    User findUserByUserId(Long userId);

    // ✅ Kiểm tra xem số điện thoại đã tồn tại chưa
    // 👉 Trả về true nếu có user với phoneNumber trùng trong DB.
    boolean existsByPhoneNumber(String phoneNumber);

    // ✅ Kiểm tra xem email đã tồn tại chưa
    // 👉 Dùng để đảm bảo email là duy nhất khi đăng ký tài khoản.
    boolean existsByEmail(String email);

    // ✅ Tìm user theo email và **fetch luôn role** (bằng annotation @EntityGraph)
    // 👉 @EntityGraph(attributePaths = "role") giúp load luôn bảng role mà không cần lazy loading.
    // ⚙️ Giúp tiết kiệm truy vấn SQL thứ 2 (tránh N+1 problem).
    @EntityGraph(attributePaths = "role")
    User findByEmail(String email);

    // ✅ Tìm user theo số điện thoại và load luôn role
    // 👉 Dùng khi cần lấy thông tin quyền (Role) cùng lúc với user (ví dụ: trong login hoặc phân quyền).
    @EntityGraph(attributePaths = "role")
    User findByPhoneNumber(String phone);

    // ✅ Truy vấn bằng JPQL: Tìm user theo email và fetch luôn role
    // 👉 Tương tự `findByEmail` nhưng dùng @Query để viết rõ câu JPQL.
    // ⚙️ SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email
    @Query("select u from User u join fetch u.role where u.email = :email")
    User findByEmailWithRole(@Param("email") String email);

    // ✅ Truy vấn để lấy tất cả user và fetch luôn các bảng liên quan (role, driver, staffs, admin)
    // 👉 Dùng trong trang quản lý user của admin — hiển thị danh sách người dùng với vai trò & thông tin chi tiết.
    // ⚙️ LEFT JOIN FETCH đảm bảo dù user không có role/staff/driver vẫn được trả về.
    // ⚙️ Ví dụ: SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.driver ...
    @Query("""
       select u from User u
       left join fetch u.role r
       left join fetch u.driver d
       left join fetch u.staffs s
       left join fetch u.admin a
       """)
    List<User> findAllWithJoins();
}
