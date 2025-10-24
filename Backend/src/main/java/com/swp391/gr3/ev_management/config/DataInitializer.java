package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.Role;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.RoleRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final DriverRepository driverRepository;

    @Value("${app.data.init.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Data initializer disabled by property 'app.data.init.enabled'");
            return;
        }

        try {
            initConnectorTypes();     // seed các loại đầu sạc phổ biến
            initRoles();              // seed các role chuẩn
            initAdmins();             // tạo 1 admin mặc định
            initVehicleModels();   // bật nếu cần demo VehicleModel (cần connector types)
            initDrivers();         // bật nếu cần demo Driver
            log.info("✅ Data initialization completed.");
        } catch (Exception ex) {
            log.error("❌ Data initialization failed: {}", ex.getMessage(), ex);
        }
    }

    // ================== CONNECTOR TYPES ==================
    private void initConnectorTypes() {
        // Một bộ nhỏ đủ dùng cho demo
        createConnectorIfNotExists("TYPE2",  "AC", "Type 2 (Mennekes)",     22.0,  false);
        createConnectorIfNotExists("CCS2",   "DC", "CCS Combo 2",            150.0, false);
        createConnectorIfNotExists("CHADEMO","DC", "CHAdeMO",                 50.0, true);
        createConnectorIfNotExists("TYPE1",  "AC", "Type 1 (SAE J1772)",      7.4,  true);
    }

    private void createConnectorIfNotExists(String code, String mode, String displayName,
                                            double defaultMaxPowerKW, boolean isDeprecated) {
        try {
            if (connectorTypeRepository.existsByCode(code)) {
                log.info("ConnectorType already exists: {}", code);
                return;
            }
            ConnectorType ct = ConnectorType.builder()
                    .code(code)
                    .mode(mode)
                    .displayName(displayName)
                    .defaultMaxPowerKW(defaultMaxPowerKW)
                    .isDeprecated(isDeprecated)
                    .build();
            connectorTypeRepository.save(ct);
            log.info("Created ConnectorType: {} ({} - {}kW)", code, mode, defaultMaxPowerKW);
        } catch (Exception e) {
            log.warn("Failed to create ConnectorType {}: {}", code, e.getMessage());
        }
    }

    // ================== VEHICLE MODELS (tùy chọn) ==================
    private void initVehicleModels() {
        createModelIfNotExists("Tesla",     "Model 3",         2023, "/images/vehicles/tesla-model3.png",   "CCS2");
        createModelIfNotExists("Tesla",     "Model Y",         2023, "/images/vehicles/tesla-modely.png",   "CCS2");
        createModelIfNotExists("Hyundai",   "Kona Electric",   2022, "/images/vehicles/hyundai-kona.png",   "CCS2");
        createModelIfNotExists("Kia",       "EV6",             2023, "/images/vehicles/kia-ev6.png",        "CCS2");
        createModelIfNotExists("VinFast",   "VF e34",          2022, "/images/vehicles/vinfast-vfe34.png",  "CCS2");
        createModelIfNotExists("Nissan",    "Leaf",            2020, "/images/vehicles/nissan-leaf.png",    "CHADEMO");
        createModelIfNotExists("Mitsubishi","Outlander PHEV",  2019, "/images/vehicles/mitsubishi-outlander.png", "TYPE1");
    }

    private void createModelIfNotExists(String brand, String model, int year, String img, String connectorCode) {
        try {
            boolean exists = vehicleModelRepository
                    .existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(brand, model, year);
            if (exists) {
                log.info("VehicleModel already exists: {} {} {}", brand, model, year);
                return;
            }

            ConnectorType connector = connectorTypeRepository.findByCode(connectorCode);
            if (connector == null) {
                log.warn("ConnectorType {} not found. Skipping model {} {} {}", connectorCode, brand, model, year);
                return;
            }

            VehicleModel vm = VehicleModel.builder()
                    .brand(brand)
                    .model(model)
                    .year(year)
                    .imageUrl((img != null && !img.isEmpty()) ? img : "default-vehicle.png")
                    .connectorType(connector)
                    .build();

            vehicleModelRepository.save(vm);
            log.info("Created VehicleModel: {} {} {} ({})", brand, model, year, connectorCode);
        } catch (Exception e) {
            log.warn("Failed to create VehicleModel {} {} {}: {}", brand, model, year, e.getMessage());
        }
    }

    // ================== ROLES ==================
    private void initRoles() {
        createRoleIfNotExists("ADMIN",  "Quản trị viên có toàn quyền truy cập hệ thống");
        createRoleIfNotExists("STAFF",  "Nhân viên nhà ga chịu trách nhiệm quản lý các hoạt động tại địa phương");
        createRoleIfNotExists("DRIVER", "Người lái xe đã đăng ký có thể sạc xe điện");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        try {
            Role existed = roleRepository.findByRoleName(roleName);
            if (existed == null) {
                Role role = new Role();
                role.setRoleName(roleName);
                role.setDescription(description);
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            } else {
                log.info("Role already exists: {}", roleName);
            }
        } catch (Exception e) {
            log.warn("Failed to create role {}: {}", roleName, e.getMessage());
        }
    }

    // ================== DEFAULT ADMIN ==================
    private void initAdmins() {
        createAdminIfNotExists(
                "0378554725",
                "vminhquang05@gmail.com",
                "123123",
                "System Admin"
        );
    }

    private void createAdminIfNotExists(String phoneNumber, String email, String rawPassword, String name) {
        try {
            boolean phoneExists = phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber);
            boolean emailExists = email != null && userRepository.existsByEmail(email);
            if (phoneExists || emailExists) {
                log.info("Admin account already exists (phone={}, email={})", phoneNumber, email);
                return;
            }

            Role adminRole = roleRepository.findByRoleName("ADMIN");
            if (adminRole == null) {
                log.warn("ADMIN role not found; skipping admin creation");
                return;
            }

            User admin = new User();
            admin.setPhoneNumber(phoneNumber);
            admin.setEmail(email);
            admin.setName(name != null ? name : "Admin");
            admin.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "Admin@123"));
            admin.setGender("M");
            admin.setDateOfBirth(LocalDate.of(1969, 4, 22));
            admin.setAddress("HCM, Vietnam");
            admin.setRole(adminRole);

            userRepository.save(admin);
            log.info("Created default admin: {} ({})", admin.getName(), admin.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to create default admin (phone={}, email={}): {}", phoneNumber, email, e.getMessage(), e);
        }
    }

    // ================== DEFAULT DRIVER (tùy chọn) ==================
    private void initDrivers() {
        createDriverIfNotExists(
                "0911111111",
                "driver@example.com",
                "123123",
                "Test Driver",
                "M",
                LocalDate.of(1995, 5, 15),
                "Hanoi, Vietnam"
        );
    }

    private void createDriverIfNotExists(String phoneNumber, String email, String rawPassword,
                                         String name, String gender, LocalDate dateOfBirth, String address) {
        try {
            boolean phoneExists = phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber);
            boolean emailExists = email != null && userRepository.existsByEmail(email);
            if (phoneExists || emailExists) {
                log.info("Driver account already exists (phone={}, email={})", phoneNumber, email);
                return;
            }

            Role driverRole = roleRepository.findByRoleName("DRIVER");
            if (driverRole == null) {
                log.warn("DRIVER role not found; skipping driver creation");
                return;
            }

            User user = new User();
            user.setPhoneNumber(phoneNumber);
            user.setEmail(email);
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "Driver@123"));
            user.setGender(gender);
            user.setDateOfBirth(dateOfBirth);
            user.setAddress(address);
            user.setRole(driverRole);
            User savedUser = userRepository.save(user);

            Driver driver = Driver.builder()
                    .user(savedUser)
                    .status(DriverStatus.ACTIVE)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            driverRepository.save(driver);

            log.info("Created default driver: {} ({}) with status {}",
                    savedUser.getName(), savedUser.getPhoneNumber(), driver.getStatus());
        } catch (Exception e) {
            log.error("Failed to create default driver (phone={}, email={}): {}", phoneNumber, email, e.getMessage(), e);
        }
    }
}
