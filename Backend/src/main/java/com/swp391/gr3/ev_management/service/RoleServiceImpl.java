package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // Đánh dấu đây là một Spring Service (chứa logic nghiệp vụ cho Role)
@RequiredArgsConstructor // Lombok sinh constructor cho tất cả field final (dùng để DI repository)
public class RoleServiceImpl implements RoleService {

    // Repository làm việc trực tiếp với bảng Role trong DB (CRUD, query, ...)
    private final RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {
        // Lấy toàn bộ bản ghi Role trong hệ thống
        // Ví dụ: ROLE_ADMIN, ROLE_STAFF, ROLE_DRIVER, ...
        return roleRepository.findAll();
    }

    @Override
    public Role findByRoleId(long l) {
        // Tìm một Role theo khóa chính roleId
        // Nếu không tìm thấy, repository sẽ trả về null (theo định nghĩa phương thức)
        return roleRepository.findByRoleId(l);
    }

    @Override
    public Role findByRoleName(String staffName) {
        // Tìm Role theo tên (roleName), ví dụ: "ADMIN", "STAFF", "DRIVER"
        // Dùng cho các logic phân quyền / gán role cho user
        return roleRepository.findByRoleName(staffName);
    }

}
