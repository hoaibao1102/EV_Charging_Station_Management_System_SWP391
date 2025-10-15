package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import com.swp391.gr3.ev_management.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    RoleService roleService;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ConnectorTypeRepository connectorTypeRepository;

    @Override
    public void run(String... args) throws Exception {
        initConnectorTypes();
        initRoles();
        initAdmins();
    }

    private void initConnectorTypes() {
        // Seed a minimal, useful set of connector types for testing VehicleModel creation
        createConnectorIfNotExists("TYPE2", "AC", "Type 2 (Mennekes)", 22.0, false);
        createConnectorIfNotExists("CCS2", "DC", "CCS Combo 2", 150.0, false);
        createConnectorIfNotExists("CHADEMO", "DC", "CHAdeMO", 50.0, true);
        createConnectorIfNotExists("TYPE1", "AC", "Type 1 (SAE J1772)", 7.4, true);
    }

    private void createConnectorIfNotExists(String code, String mode, String displayName, double defaultMaxPowerKW, boolean isDeprecated) {
        if (connectorTypeRepository.existsByCode(code)) {
            System.out.println("ℹ️ ConnectorType already exists: " + code);
            return;
        }
        ConnectorType ct = ConnectorType.builder()
                .code(code)
                .mode(mode)
                .displayName(displayName)
                .defaultMaxPowerKW(defaultMaxPowerKW)
                .isDeprecated(isDeprecated)
                .build();
        ConnectorType saved = connectorTypeRepository.save(ct);
        System.out.println("✅ Created ConnectorType: " + saved.getCode() + " (ID=" + saved.getConnectorTypeId() + ")");
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

    // ===== Admin seeding =====
    private void initAdmins() {
        // You can change these defaults or load from env variables
        // Example: System.getenv("DEFAULT_ADMIN_PHONE")
        createAdminIfNotExists(
                "0999999999",
                "admin@example.com",
                "Admin@123",
                "System Admin"
        );

        // (Optional) Add more default admins by duplicating the call above with different credentials
        // createAdminIfNotExists("0888888888", "admin2@example.com", "Admin@123", "Second Admin");
    }

    private void createAdminIfNotExists(String phoneNumber, String email, String rawPassword, String name) {
        boolean phoneExists = phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber);
        boolean emailExists = email != null && userRepository.existsByEmail(email);
        if (phoneExists || emailExists) {
            System.out.printf("ℹ️ Admin account already exists (phone=%s, email=%s)\n", phoneNumber, email);
            return;
        }

        Role adminRole = roleRepository.findByRoleName("ADMIN");
        if (adminRole == null) {
            System.out.println("❌ ADMIN role not found; skipping admin creation");
            return;
        }

        User admin = new User();
        admin.setPhoneNumber(phoneNumber);
        admin.setEmail(email);
        admin.setName(name != null ? name : "Admin");
        admin.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "Admin@123"));
        admin.setGender("M");
        admin.setDateOfBirth(LocalDate.of(1969, 04, 22));
        admin.setAddress("HCM, Vietnam");
        admin.setRole(adminRole);

        userRepository.save(admin);
        System.out.printf("✅ Created default admin: %s (%s)\n", admin.getName(), admin.getPhoneNumber());
    }
}
