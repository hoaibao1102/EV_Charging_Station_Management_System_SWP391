package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // ✅ Repository này quản lý entity "Role" — đại diện cho vai trò (quyền hạn) của người dùng như ADMIN, STAFF, DRIVER,...
    // ✅ Kế thừa JpaRepository => có sẵn các phương thức CRUD cơ bản (save, findAll, findById, deleteById,...)

    /**
     * ✅ Tìm một vai trò (Role) theo tên vai trò.
     *
     * 👉 Ý nghĩa:
     * - Dùng khi hệ thống cần gán vai trò cho người dùng (ví dụ khi đăng ký tài khoản hoặc phân quyền).
     * - Ví dụ: cần tìm role "ADMIN" hoặc "DRIVER" để gán cho một user mới.
     *
     * ⚙️ Query tự động được Spring Data JPA sinh ra:
     * SELECT * FROM role WHERE role_name = :roleName LIMIT 1;
     *
     * 💡 `roleName` thường là chuỗi: "ADMIN", "STAFF", "DRIVER", v.v...
     *
     * @param roleName tên của vai trò cần tìm
     * @return đối tượng Role tương ứng (nếu tồn tại), nếu không có thì trả về null
     */
    Role findByRoleName(String roleName);


    /**
     * ✅ Tìm một vai trò (Role) theo ID.
     *
     * 👉 Ý nghĩa:
     * - Dùng để truy xuất thông tin role dựa theo khóa chính (roleId).
     * - Hữu ích khi bạn có ID nhưng cần lấy chi tiết vai trò (ví dụ: khi load từ bảng người dùng có role_id).
     *
     * ⚙️ Query tự động được Spring Data JPA sinh ra:
     * SELECT * FROM role WHERE role_id = :roleId;
     *
     * 💡 Khác với `findById()` mặc định của JpaRepository,
     *    hàm này cho phép bạn đặt tên rõ ràng và không cần dùng Optional.
     *
     * @param roleId ID của vai trò (Primary Key)
     * @return đối tượng Role tương ứng, hoặc null nếu không tìm thấy
     */
    Role findByRoleId(Long roleId);

}
