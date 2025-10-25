package com.swp391.gr3.ev_management.config;

import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.*;
import com.swp391.gr3.ev_management.repository.*;
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
    private final AdminRepository adminRepository;
    private final StaffsRepository staffsRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final ChargingPointRepository chargingPointRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;

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
            initAdmins();             // tạo 1 admin mặc định + map bảng Admin
            initStaffs();             // tạo 1 staff mặc định + map bảng Staffs
            initVehicleModels();      // seed VehicleModel (cần connector types)
            initDrivers();            // seed Driver
            initChargingStations();   // seed trạm sạc theo Seed_Data
            initChargingPoints();     // seed điểm sạc theo Seed_Data
            initSlotAvailability();   // seed mẫu slot theo Seed_Data

            log.info("✅ Data initialization completed.");
        } catch (Exception ex) {
            log.error("❌ Data initialization failed: {}", ex.getMessage(), ex);
        }
    }

    // ================== STATIONS ==================
    private void initChargingStations() {
        createStationIfNotExists(
                "VinFast SmartCharge - Quận 1",
                "15 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh",
                10.776889, 106.700806,
                "24/7"

        );
        createStationIfNotExists(
                "EVN Station - Thủ Đức City",
                "268 Võ Văn Ngân, Thủ Đức, TP. Hồ Chí Minh",
                10.84942, 106.76973,
                "06:00-23:00"

        );
        createStationIfNotExists(
                "VinFast Station - Landmark 81",
                "720A Điện Biên Phủ, Bình Thạnh, TP. Hồ Chí Minh",
                10.794187, 106.72164,
                "24/7"

        );
    }

    private void createStationIfNotExists(String name, String address, double lat, double lng,
                                          String operatingHours) {
        try {
            if (chargingStationRepository.existsByStationNameIgnoreCaseAndAddressIgnoreCase(name, address)) {
                log.info("ChargingStation already exists: {} - {}", name, address);
                return;
            }
            var station = ChargingStation.builder()
                    .stationName(name)
                    .address(address)
                    .latitude(lat)
                    .longitude(lng)
                    .operatingHours(operatingHours)
                    .status(ChargingStationStatus.ACTIVE)
                    .build();
            chargingStationRepository.save(station);
            log.info("Created ChargingStation: {}", name);
        } catch (Exception e) {
            log.warn("Failed to create ChargingStation {}: {}", name, e.getMessage());
        }
    }

    // ================== CHARGING POINTS ==================
    private void initChargingPoints() {
        // Seed 10 points for station 1 as sample
        var stationOpt = chargingStationRepository.findByStationName("VinFast SmartCharge - Quận 1");
        if (stationOpt.isEmpty()) {
            log.warn("Base station for points not found; skip point seeding");
            return;
        }
        var station = stationOpt.get();

        createPointIfNotExists(station, "S1-A1", ChargingPointStatus.AVAILABLE, "S1-A1-2025", 150);
        createPointIfNotExists(station, "S1-A2", ChargingPointStatus.AVAILABLE, "S1-A2-2025", 22);
        createPointIfNotExists(station, "S1-A3", ChargingPointStatus.OCCUPIED, "S1-A3-2025", 50);
        createPointIfNotExists(station, "S1-A4", ChargingPointStatus.AVAILABLE, "S1-A4-2025", 250);
        createPointIfNotExists(station, "S1-A5", ChargingPointStatus.AVAILABLE, "S1-A5-2025", 60);
        createPointIfNotExists(station, "S1-A6", ChargingPointStatus.OCCUPIED, "S1-A6-2025", 7);
        createPointIfNotExists(station, "S1-A7", ChargingPointStatus.AVAILABLE, "S1-A7-2025", 150);
        createPointIfNotExists(station, "S1-A8", ChargingPointStatus.MAINTENANCE, "S1-A8-2025", 22);
        createPointIfNotExists(station, "S1-A9", ChargingPointStatus.AVAILABLE, "S1-A9-2025", 50);
        createPointIfNotExists(station, "S1-A10", ChargingPointStatus.AVAILABLE, "S1-A10-2025", 250);
    }

    private void createPointIfNotExists(
            ChargingStation station,
            String pointNumber,
            ChargingPointStatus pointStatus,
            String serialNumber,
            double maxPowerKW
    ) {
        try {
            // Check duplicates by pointNumber within station or by serialNumber
            var existedByPoint = chargingPointRepository.findByStation_StationIdAndPointNumber(station.getStationId(), pointNumber);
            if (existedByPoint.isPresent()) {
                log.info("ChargingPoint already exists: {}", pointNumber);
                return;
            }
            var existedBySerial = chargingPointRepository.findBySerialNumber(serialNumber);
            if (existedBySerial.isPresent()) {
                log.info("ChargingPoint already exists by serial: {}", serialNumber);
                return;
            }

            var connector = mapConnectorByPower(maxPowerKW);
            if (connector == null) {
                log.warn("No suitable ConnectorType for power {}kW, skipping point {}", maxPowerKW, pointNumber);
                return;
            }

            var cp = ChargingPoint.builder()
                    .station(station)
                    .connectorType(connector)
                    .pointNumber(pointNumber)
                    .status(pointStatus)
                    .serialNumber(serialNumber)
                    .installationDate(LocalDateTime.now())
                    .lastMaintenanceDate(LocalDateTime.now())
                    .maxPowerKW(maxPowerKW)
                    .build();
            chargingPointRepository.save(cp);
            log.info("Created ChargingPoint: {} ({}kW)", pointNumber, maxPowerKW);
        } catch (Exception e) {
            log.warn("Failed to create ChargingPoint {}: {}", pointNumber, e.getMessage());
        }
    }

    private ConnectorType mapConnectorByPower(double maxPowerKW) {
        // Simple heuristic mapping derived from sample data
        if (maxPowerKW >= 120) return connectorTypeRepository.findByCode("CCS2");
        if (maxPowerKW >= 45 && maxPowerKW < 120) return connectorTypeRepository.findByCode("CHADEMO");
        if (maxPowerKW <= 8) return connectorTypeRepository.findByCode("TYPE1");
        return connectorTypeRepository.findByCode("TYPE2");
    }

    // ================== SLOT AVAILABILITY ==================
    private void initSlotAvailability() {
        try {
            var stationOpt = chargingStationRepository.findByStationName("VinFast SmartCharge - Quận 1");
            if (stationOpt.isEmpty()) {
                log.warn("Base station not found; skip slot availability seeding");
                return;
            }
            var station = stationOpt.get();

            // Ensure a SlotConfig exists for station
            var existingConfig = slotConfigRepository.findByStation_StationId(station.getStationId());
            if (existingConfig == null) {
                var cfg = SlotConfig.builder()
                        .station(station)
                        .slotDurationMin(60)
                        .activeFrom(java.time.LocalDate.now().atStartOfDay())
                        .activeExpire(java.time.LocalDate.now().plusYears(1).atStartOfDay())
                        .isActive(SlotConfigStatus.ACTIVE)
                        .build();
                existingConfig = slotConfigRepository.save(cfg);
                log.info("Created SlotConfig for station {}", station.getStationName());
            }

            // Recreate 24 templates for a base day (today)
            var baseStart = java.time.LocalDate.now().atStartOfDay();
            var baseEnd = baseStart.plusDays(1);
            slotTemplateRepository.deleteByConfig_ConfigIdAndStartTimeBetween(existingConfig.getConfigId(), baseStart, baseEnd);

            var templates = new java.util.ArrayList<SlotTemplate>();
            for (int i = 0; i < 24; i++) {
                var st = SlotTemplate.builder()
                        .config(existingConfig)
                        .slotIndex(i + 1)
                        .startTime(baseStart.plusHours(i))
                        .endTime(baseStart.plusHours(i + 1))
                        .build();
                templates.add(slotTemplateRepository.save(st));
            }
            log.info("Created {} SlotTemplates for config {}", templates.size(), existingConfig.getConfigId());

            // Seed availability for a specific date from Seed_Data: 2025-10-24
            var targetDate = java.time.LocalDate.parse("2025-10-24").atStartOfDay();
            var connector = connectorTypeRepository.findByCode("TYPE2");
            if (connector == null) connector = connectorTypeRepository.findByCode("CCS2");

            for (var t : templates) {
                boolean exists = slotAvailabilityRepository
                        .existsByTemplate_TemplateIdAndConnectorType_ConnectorTypeIdAndDate(
                                t.getTemplateId(),
                                connector != null ? connector.getConnectorTypeId() : null,
                                targetDate
                        );
                if (exists) continue;

                var sa = SlotAvailability.builder()
                        .template(t)
                        .connectorType(connector)
                        .status(SlotStatus.AVAILABLE)
                        .date(targetDate)
                        .build();
                slotAvailabilityRepository.save(sa);
            }
            log.info("Seeded slot availability for {} templates on date {}", templates.size(), targetDate.toLocalDate());
        } catch (Exception e) {
            log.warn("Failed to seed slot availability: {}", e.getMessage());
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
            User adminUser;
            if (phoneExists || emailExists) {
                // Nếu user đã tồn tại, lấy ra để đảm bảo map sang bảng Admin
                adminUser = email != null ? userRepository.findByEmail(email) : userRepository.findByPhoneNumber(phoneNumber);
                log.info("Admin User already exists (phone={}, email={})", phoneNumber, email);
            } else {
                Role adminRole = roleRepository.findByRoleName("ADMIN");
                if (adminRole == null) {
                    log.warn("ADMIN role not found; skipping admin creation");
                    return;
                }

                adminUser = new User();
                adminUser.setPhoneNumber(phoneNumber);
                adminUser.setEmail(email);
                adminUser.setName(name != null ? name : "Admin");
                adminUser.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "123123"));
                adminUser.setGender("M");
                adminUser.setDateOfBirth(LocalDate.of(1969, 4, 22));
                adminUser.setAddress("HCM, Vietnam");
                adminUser.setRole(adminRole);

                adminUser = userRepository.save(adminUser);
                log.info("Created default admin USER: {} ({})", adminUser.getName(), adminUser.getPhoneNumber());
            }

            // Map sang bảng Admin nếu chưa có
            if (!adminRepository.existsByUser_UserId(adminUser.getUserId())) {
                Admin admin = Admin.builder()
                        .user(adminUser)
                        .roleLevel("ADMIN")
                        .build();
                adminRepository.save(admin);
                log.info("Mapped USER {} to ADMIN table (roleLevel={})", adminUser.getUserId(), "ADMIN");
            } else {
                log.info("Admin mapping already exists for userId={}", adminUser.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to create default admin (phone={}, email={}): {}", phoneNumber, email, e.getMessage(), e);
        }
    }

    // ================== DEFAULT STAFF (do Admin tạo) ==================
    private void initStaffs() {
        createStaffIfNotExists(
                "0900000001",
                "staff1@example.com",
                "123123",
                "Default Staff",
                "F",
                LocalDate.of(1998, 1, 1),
                "HCM, Vietnam",
                "OPERATOR"
        );
    }

    private void createStaffIfNotExists(String phoneNumber, String email, String rawPassword,
                                        String name, String gender, LocalDate dateOfBirth, String address,
                                        String roleAtStation) {
        try {
            boolean phoneExists = phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber);
            boolean emailExists = email != null && userRepository.existsByEmail(email);

            User staffUser;
            if (phoneExists || emailExists) {
                staffUser = email != null ? userRepository.findByEmail(email) : userRepository.findByPhoneNumber(phoneNumber);
                log.info("Staff user already exists (phone={}, email={})", phoneNumber, email);
            } else {
                Role staffRole = roleRepository.findByRoleName("STAFF");
                if (staffRole == null) {
                    log.warn("STAFF role not found; skipping staff creation");
                    return;
                }

                staffUser = new User();
                staffUser.setPhoneNumber(phoneNumber);
                staffUser.setEmail(email);
                staffUser.setName(name != null ? name : "Staff");
                staffUser.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "Staff@123"));
                staffUser.setGender(gender);
                staffUser.setDateOfBirth(dateOfBirth);
                staffUser.setAddress(address);
                staffUser.setRole(staffRole);
                staffUser = userRepository.save(staffUser);
                log.info("Created default STAFF USER: {} ({})", staffUser.getName(), staffUser.getPhoneNumber());
            }

            // Tạo bản ghi Staffs nếu chưa có
            if (staffUser.getStaffs() == null) {
                Staffs staffs = Staffs.builder()
                        .user(staffUser)
                        .status(StaffStatus.ACTIVE)
                        .roleAtStation(roleAtStation)
                        .build();
                staffsRepository.save(staffs);
                log.info("Mapped USER {} to STAFFS table (status={}, roleAtStation={})",
                        staffUser.getUserId(), StaffStatus.ACTIVE, roleAtStation);
            } else {
                log.info("Staffs mapping already exists for userId={}", staffUser.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to create default staff (phone={}, email={}): {}", phoneNumber, email, e.getMessage(), e);
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
