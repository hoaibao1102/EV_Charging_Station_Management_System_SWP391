package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.service.RoleService;
import com.swp391.gr3.ev_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    RoleService roleService;

    @Override
    public void run(String... args) throws Exception {
        //initRoles();
    }

    private void initRoles() {
        createRoleIfNotExists("ADMIN", "Quản trị viên có toàn quyền truy cập hệ thống");
        createRoleIfNotExists("STAFF", "Nhân viên nhà ga chịu trách nhiệm quản lý các hoạt động tại địa phương");
        createRoleIfNotExists("DRIVER", "Người lái xe đã đăng ký có thể sạc xe điện");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (roleService.findByRoleName(roleName) == null) {
            Role role = new Role();
            role.setRoleName(roleName);
            role.setDescription(description);
            roleService.addRole(role);
            System.out.println("✅ Created default role: " + roleName);
        } else {
            System.out.println("ℹ️ Role already exists: " + roleName);
        }
    }


}
