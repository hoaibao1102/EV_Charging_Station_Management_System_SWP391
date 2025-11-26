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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final TransactionTemplate transactionTemplate;
    private final TariffRepository tariffRepository;
    private final StationStaffRepository stationStaffRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final DriverViolationRepository driverViolationRepository;
    private final DriverViolationTripletRepository driverViolationTripletRepository;
    private final PolicyRepository policyRepository;


    @Value("${app.data.init.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Data initializer disabled by property 'app.data.init.enabled'");
            return;
        }

        try {
            initConnectorTypes();     // seed các loại đầu sạc phổ biến
            initRoles();              // seed các role chuẩn
            initAdmins();             // tạo 1 admin mặc định + map bảng Admin
            initVehicleModels();      // seed VehicleModel (cần connector types)
//            initDrivers();            // seed Driver
            initChargingStations();   // seed trạm sạc theo Seed_Data
            initChargingPoints();     // seed điểm sạc theo Seed_Data
//            initSlotAvailability();   // seed mẫu slot theo Seed_Data
            initStaffs();             // tạo 1 staff mặc định + map bảng Staffs
            initTariffs();            // seed bảng Tariff
            initPaymentMethods();     // seed bảng PaymentMethod
            initBanDrivers();
            initBannedDriverViolations();
            initTwoViolationsFor0987456321();
            initPolicies();


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
        createStationIfNotExists(
                "EVN Station - Quận 7",
                "101 Tôn Dật Tiên, Phú Mỹ Hưng, Quận 7, TP. Hồ Chí Minh",
                10.729884, 106.718139,
                "06:00-23:00"
        );

        createStationIfNotExists(
                "VinFast Station - Aeon Mall Tân Phú",
                "30 Bờ Bao Tân Thắng, Sơn Kỳ, Tân Phú, TP. Hồ Chí Minh",
                10.799926, 106.615821,
                "08:00-22:00"
        );

        createStationIfNotExists(
                "EVN Station - Quận 3",
                "190 Nam Kỳ Khởi Nghĩa, Quận 3, TP. Hồ Chí Minh",
                10.781219, 106.688545,
                "24/7"
        );

        createStationIfNotExists(
                "VinFast SmartCharge - Quận 10",
                "285A Cách Mạng Tháng Tám, Quận 10, TP. Hồ Chí Minh",
                10.772679, 106.667207,
                "07:00-22:00"
        );

        createStationIfNotExists(
                "EVN Station - Bình Tân",
                "1/1A Kinh Dương Vương, Bình Tân, TP. Hồ Chí Minh",
                10.751355, 106.603772,
                "24/7"
        );

        createStationIfNotExists(
                "VinFast Station - GigaMall Thủ Đức",
                "242 Phạm Văn Đồng, Hiệp Bình Chánh, Thủ Đức, TP. Hồ Chí Minh",
                10.846884, 106.732487,
                "08:00-22:00"
        );

        createStationIfNotExists(
                "EVN Station - Quận 5",
                "55 An Dương Vương, Quận 5, TP. Hồ Chí Minh",
                10.757202, 106.666325,
                "06:00-23:00"
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
        seedPointsForStation("VinFast SmartCharge - Quận 1",        "S1");
        seedPointsForStation("EVN Station - Thủ Đức City",          "S2");
        seedPointsForStation("VinFast Station - Landmark 81",       "S3");
        seedPointsForStation("EVN Station - Quận 7",                "S4");
        seedPointsForStation("VinFast Station - Aeon Mall Tân Phú", "S5");
        seedPointsForStation("EVN Station - Quận 3",                "S6");
        seedPointsForStation("VinFast SmartCharge - Quận 10",       "S7");
        seedPointsForStation("EVN Station - Bình Tân",              "S8");
        seedPointsForStation("VinFast Station - GigaMall Thủ Đức",  "S9");
        seedPointsForStation("EVN Station - Quận 5",                "S10");
    }

    /**
     * Seed 10 charging points cho 1 trạm với prefix Sx (S1, S2, ...)
     */
    private void seedPointsForStation(String stationName, String codePrefix) {
        var stationOpt = chargingStationRepository.findByStationName(stationName);
        if (stationOpt.isEmpty()) {
            log.warn("Base station for points not found: {} ; skip point seeding", stationName);
            return;
        }
        var station = stationOpt.get();

        // Bạn có thể tuỳ chỉnh status / công suất giống mẫu S1 ở đây
        createPointIfNotExists(station, codePrefix + "-A1",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A1-2025", 150);
        createPointIfNotExists(station, codePrefix + "-A2",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A2-2025", 22);
        createPointIfNotExists(station, codePrefix + "-A3",  ChargingPointStatus.OCCUPIED,    codePrefix + "-A3-2025", 50);
        createPointIfNotExists(station, codePrefix + "-A4",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A4-2025", 250);
        createPointIfNotExists(station, codePrefix + "-A5",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A5-2025", 60);
        createPointIfNotExists(station, codePrefix + "-A6",  ChargingPointStatus.OCCUPIED,    codePrefix + "-A6-2025", 7);
        createPointIfNotExists(station, codePrefix + "-A7",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A7-2025", 150);
        createPointIfNotExists(station, codePrefix + "-A8",  ChargingPointStatus.MAINTENANCE, codePrefix + "-A8-2025", 22);
        createPointIfNotExists(station, codePrefix + "-A9",  ChargingPointStatus.AVAILABLE,   codePrefix + "-A9-2025", 50);
        createPointIfNotExists(station, codePrefix + "-A10", ChargingPointStatus.AVAILABLE,   codePrefix + "-A10-2025", 250);
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
    private void seedStation(ChargingStation station) {
        try {
            // 1. Ensure SlotConfig exists
            var existingConfig = slotConfigRepository.findByStation_StationId(station.getStationId());
            if (existingConfig == null) {
                var cfg = SlotConfig.builder()
                        .station(station)
                        .slotDurationMin(60)
                        .activeFrom(LocalDate.now().atStartOfDay())
                        .activeExpire(LocalDate.now().plusYears(1).atStartOfDay())
                        .isActive(SlotConfigStatus.ACTIVE)
                        .build();
                existingConfig = slotConfigRepository.save(cfg);
                log.info("Created SlotConfig for station {}", station.getStationName());
            }

            final var config = existingConfig;
            final var baseStart = LocalDate.now().atStartOfDay();
            final var baseEnd   = baseStart.plusDays(1);
            final var targetDate = LocalDate.parse("2025-10-24").atStartOfDay();

            // Bọc transaction RIÊNG cho reset + seed
            transactionTemplate.executeWithoutResult(status -> {
                // 1) XÓA AVAILABILITY TRONG NGÀY (con trước)
                slotAvailabilityRepository.deleteByConfigIdAndDateRange(
                        config.getConfigId(), baseStart, baseEnd);
                slotAvailabilityRepository.flush(); // ép DB thực thi

                // 2) XÓA TEMPLATE TRONG NGÀY (KHÔNG bulk delete)
                var oldTemplates = slotTemplateRepository
                        .findByConfig_ConfigIdAndStartTimeBetween(config.getConfigId(), baseStart, baseEnd);
                if (!oldTemplates.isEmpty()) {
                    slotTemplateRepository.deleteAll(oldTemplates);
                    slotTemplateRepository.flush();
                }

                // C) TẠO LẠI 24 TEMPLATE
                var templates = new ArrayList<SlotTemplate>(24);
                for (int i = 0; i < 24; i++) {
                    var st = SlotTemplate.builder()
                            .config(config)
                            .slotIndex(i + 1)
                            .startTime(baseStart.plusHours(i))
                            .endTime(baseStart.plusHours(i + 1))
                            .build();
                    templates.add(slotTemplateRepository.save(st));
                }
                log.info("Created {} SlotTemplates for station {}", templates.size(), station.getStationName());

                // D) LẤY TẤT CẢ CHARGING POINTS CỦA TRẠM (Không lọc theo Type nữa)
                List<ChargingPoint> points = chargingPointRepository.findByStation_StationId(station.getStationId());

                if (points.isEmpty()) {
                    log.warn("No charging points found for station {} - Skipping availability seeding", station.getStationName());
                    return;
                }
                if (points.isEmpty()) {
                    log.warn("No charging points found for station {} - Skipping availability seeding", station.getStationName());
                    return;
                }

                // E) SEED AVAILABILITY
                var toSave = new ArrayList<SlotAvailability>(templates.size() * points.size());
                for (var t : templates) {
                    for (var p : points) {
                        toSave.add(SlotAvailability.builder()
                                .template(t)
                                .chargingPoint(p)
                                .status(SlotStatus.AVAILABLE)
                                .date(targetDate)
                                .build());
                    }
                }
                slotAvailabilityRepository.saveAll(toSave);
                log.info("Seeded {} slot availability rows for station {}", toSave.size(), station.getStationName());
            });

        } catch (Exception e) {
            log.error("Failed to seed data for station: " + station.getStationName(), e);
        }
    }
    private void initSlotAvailability() {
        // Danh sách tên các trạm cần seed data
        List<String> stationNames = List.of(
                "VinFast SmartCharge - Quận 1",
                "EVN Station - Thủ Đức City",
                "VinFast Station - Landmark 81",
                "EVN Station - Quận 7",
                "VinFast Station - Aeon Mall Tân Phú",
                "EVN Station - Quận 3",
                "VinFast SmartCharge - Quận 10",
                "EVN Station - Bình Tân",
                "VinFast Station - GigaMall Thủ Đức",
                "EVN Station - Quận 5"
        );

        log.info("Starting batch seed slot availability...");

        for (String name : stationNames) {
            var stationOpt = chargingStationRepository.findByStationName(name);

            if (stationOpt.isPresent()) {
                // Gọi hàm helper đã tách ở bước 1
                seedStation(stationOpt.get());
            } else {
                log.warn("Station not found: {}", name);
            }
        }

        log.info("Finished batch seed slot availability.");
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
            if (connectorTypeRepository.existsByCodeAndMode(code, mode)) {
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
        createModelIfNotExists("Tesla",     "Model 3",         2023, "/images/vehicles/tesla-model3.png",   "tesla-model3",   "CCS2",75, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("Tesla",     "Model Y",         2023, "/images/vehicles/tesla-modely.png",   "tesla-modely",   "TYPE2",80, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("Hyundai",   "Kona Electric",   2022, "/images/vehicles/hyundai-kona.png",   "hyundai-kona",   "CCS2", 64, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("Kia",       "EV6",             2023, "/images/vehicles/kia-ev6.png",        "kia-ev6",       "CCS2", 77, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("VinFast",   "VF e34",          2022, "/images/vehicles/vinfast-vfe34.png",  "vinfast-vfe34",   "CCS2",42, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("Nissan",    "Leaf",            2020, "/images/vehicles/nissan-leaf.png",    "nissan-leaf",    "CHADEMO",40, VehicleModelStatus.ACTIVE);
        createModelIfNotExists("Mitsubishi","Outlander PHEV",  2019, "/images/vehicles/mitsubishi-outlander.png", "mitsubishi-outlander", "TYPE1",23.8, VehicleModelStatus.ACTIVE);

    }

    private void createModelIfNotExists(String brand, String model, int year, String img, String imagePublicId, String connectorCode, double batteryCapacityKWh, VehicleModelStatus status) {
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
                    .imagePublicId((imagePublicId != null && !imagePublicId.isEmpty()) ? imagePublicId : "default-vehicle.png")
                    .connectorType(connector)
                    .status(status)
                    .batteryCapacityKWh(batteryCapacityKWh)
                    .build();

            vehicleModelRepository.save(vm);
            log.info("Created VehicleModel: {} {} {} {} ({})", brand, model, year, status, connectorCode);
        } catch (Exception e) {
            log.warn("Failed to create VehicleModel {} {} {} {}: {}", brand, model, year, status, e.getMessage());
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
        log.info("Starting staff seeding...");

        // Staff 1 -> VinFast SmartCharge - Quận 1
        createStaffIfNotExists("0900000011", "dinhhuy09349@gmail.com", "123123",
                "Dinh huy Staff", "F", LocalDate.of(1998, 1, 1), "HCM, Vietnam",
                "OPERATOR", "VinFast SmartCharge - Quận 1");

        // Staff 2 -> EVN Station - Thủ Đức City
        createStaffIfNotExists("0900000002", "staff2@example.com", "123123",
                "Station Staff 2", "M", LocalDate.of(1997, 2, 2), "HCM, Vietnam",
                "OPERATOR", "EVN Station - Thủ Đức City");

        // Staff 3 -> VinFast Station - Landmark 81
        createStaffIfNotExists("0900000003", "staff3@example.com", "123123",
                "Station Staff 3", "F", LocalDate.of(1999, 3, 3), "HCM, Vietnam",
                "SUPPORT", "VinFast Station - Landmark 81");

        // Staff 4 -> EVN Station - Quận 7
        createStaffIfNotExists("0900000004", "staff4@example.com", "123123",
                "Station Staff 4", "M", LocalDate.of(1996, 4, 4), "HCM, Vietnam",
                "OPERATOR", "EVN Station - Quận 7");

        // Staff 5 -> VinFast Station - Aeon Mall Tân Phú
        createStaffIfNotExists("0900000005", "staff5@example.com", "123123",
                "Station Staff 5", "F", LocalDate.of(2000, 5, 5), "HCM, Vietnam",
                "SUPPORT", "VinFast Station - Aeon Mall Tân Phú");

        // Staff 6 -> EVN Station - Quận 3
        createStaffIfNotExists("0900000006", "staff6@example.com", "123123",
                "Station Staff 6", "F", LocalDate.of(1995, 6, 6), "HCM, Vietnam",
                "OPERATOR", "EVN Station - Quận 3");

        log.info("Finished staff seeding.");
    }

    @Transactional
    public void createStaffIfNotExists(String phoneNumber, String email, String rawPassword,
                                       String name, String gender, LocalDate dateOfBirth, String address,
                                       String roleAtStation, String targetStationName) { // <--- Thêm tham số này
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
                // Lưu ý: Mình set cứng pass là Staff@123 để bạn dễ login test,
                // vì passwordHash trong SQL bạn gửi không thể decrypt ngược lại thành raw password được.
                staffUser.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "123123"));
                staffUser.setGender(gender);
                staffUser.setDateOfBirth(dateOfBirth);
                staffUser.setAddress(address);
                staffUser.setRole(staffRole);
                staffUser = userRepository.save(staffUser);
                log.info("Created default STAFF USER: {} ({})", staffUser.getName(), staffUser.getPhoneNumber());
            }

            // Tạo bản ghi Staffs nếu chưa có
            Staffs staff = staffUser.getStaffs();
            if (staff == null) {
                staff = Staffs.builder()
                        .user(staffUser)
                        .status(StaffStatus.ACTIVE)
                        .roleAtStation(roleAtStation)
                        .build();
                staff = staffsRepository.save(staff);
                log.info("Mapped USER {} to STAFFS table", staffUser.getUserId());
            }

            // ✅ Gán station dựa trên tham số truyền vào
            var stationOpt = chargingStationRepository.findByStationName(targetStationName); // <--- Dùng tham số
            if (stationOpt.isPresent()) {
                var station = stationOpt.get();
                final Staffs currentStaff = staff;

                boolean alreadyAssigned = stationStaffRepository
                        .existsByStaff_StaffIdAndStation_StationIdAndUnassignedAtIsNull(
                                currentStaff.getStaffId(), station.getStationId());

                if (!alreadyAssigned) {
                    StationStaff link = StationStaff.builder()
                            .staff(currentStaff)
                            .station(station)
                            .assignedAt(LocalDateTime.now())
                            .unassignedAt(null)
                            .build();

                    stationStaffRepository.save(link);
                    log.info("Linked staff {} to station '{}'", staffUser.getName(), station.getStationName());
                }
            } else {
                log.warn("Station '{}' not found, skipping assignment for staff {}", targetStationName, staffUser.getName());
            }

        } catch (Exception e) {
            log.error("Failed to create staff {}: {}", phoneNumber, e.getMessage());
        }
    }


    // ================== DEFAULT DRIVER (tùy chọn) ==================
    private void initDrivers() {
        createDriverIfNotExists(
                "0911111111",
                "hoaibaole.qng@gmail.com",
                "123123",
                "Test Driver",
                "M",
                LocalDate.of(1995, 5, 15),
                "Hanoi, Vietnam",
                DriverStatus.ACTIVE
        );
    }

    // ================== DEFAULT BAN DRIVER (tùy chọn) ==================
    private void initBanDrivers() {
        createDriverIfNotExists(
                "0902914788", "wangvu05fpt@gmail.com", "123123",
                "Test Ban Driver 1", "M", LocalDate.of(1995, 5, 15),
                "Hanoi, Vietnam", DriverStatus.BANNED
        );

        createDriverIfNotExists(
                "0321456987", "edwardwright.171198@gmail.com", "123123",
                "Test Ban Driver 2", "F", LocalDate.of(2005, 3, 6),
                "Hanoi, Vietnam", DriverStatus.BANNED
        );

        createDriverIfNotExists(
                "0987456321", "leekianhong938@gmail.com", "123123",
                "Test Ban Driver 3", "F", LocalDate.of(2001, 5, 29),
                "Hanoi, Vietnam", DriverStatus.ACTIVE
        );
        createDriverIfNotExists(
                "0919298296", "doanhuy0852@gmail.com", "123123",
                "HuyDoan", "F", LocalDate.of(2001, 5, 29),
                "Hanoi, Vietnam", DriverStatus.ACTIVE
        );
    }


    private void createDriverIfNotExists(String phoneNumber, String email, String rawPassword,
                                         String name, String gender, LocalDate dateOfBirth,
                                         String address, DriverStatus status) {
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
                    .status(status != null ? status : DriverStatus.ACTIVE)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            driverRepository.save(driver);

            log.info("Created default driver: {} ({}) with status {}",
                    savedUser.getName(), savedUser.getPhoneNumber(), driver.getStatus());
        } catch (Exception e) {
            log.error("Failed to create default driver (phone={}, email={}): {}", phoneNumber, email, e.getMessage(), e);
        }
    }

    // ================== TARIFFS ==================
    private void initTariffs() {
        try {
            // Khung thời gian áp dụng mẫu (cả năm nay)
            var from = LocalDate.now().withDayOfYear(1).atStartOfDay();
            var to   = LocalDate.now().withMonth(12).withDayOfMonth(31).atTime(23,59,59);

            // Ví dụ giá tham khảo theo loại đầu sạc
            createTariffIfNotExists("TYPE1",   3200.0, 2000, "VND", from, to);   // AC 7.4kW
            createTariffIfNotExists("TYPE2",   3800.0, 2000, "VND", from, to);   // AC 22kW
            createTariffIfNotExists("CHADEMO", 5200.0, 2500, "VND", from, to);   // DC 50kW
            createTariffIfNotExists("CCS2",    6500.0, 2500, "VND", from, to);   // DC 150kW

            log.info("Created default tariffs if missing.");
        } catch (Exception e) {
            log.warn("Failed to seed tariffs: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo tariff cho 1 connector nếu chưa có bản ghi trùng (cùng connector + khoảng thời gian + giá + tiền tệ).
     * Nếu đã có, bỏ qua để tránh trùng lặp khi khởi động nhiều lần.
     */
    private void createTariffIfNotExists(String connectorCode,
                                         double pricePerKWh,
                                         double pricePerMin,
                                         String currency,
                                         LocalDateTime effectiveFrom,
                                         LocalDateTime effectiveTo) {
        try {
            ConnectorType connector = connectorTypeRepository.findByCode(connectorCode);
            if (connector == null) {
                log.warn("ConnectorType {} not found. Skip tariff seeding.", connectorCode);
                return;
            }

            // Kiểm tra xem đã có bản ghi giống hệt chưa (đơn giản, tránh duplicate khi restart)
            boolean existsSame = tariffRepository.findAll().stream().anyMatch(t ->
                    t.getConnectorType().getConnectorTypeId().equals(connector.getConnectorTypeId())
                            && t.getCurrency().equalsIgnoreCase(currency)
                            && t.getPricePerKWh() == pricePerKWh
                            && t.getPricePerMin() == pricePerMin
                            && t.getEffectiveFrom().equals(effectiveFrom)
                            && t.getEffectiveTo().equals(effectiveTo)
            );
            if (existsSame) {
                log.info("Tariff already exists for {} ({} {} {} VND from {} to {})",
                        connectorCode, pricePerKWh, pricePerMin, currency, effectiveFrom, effectiveTo);
                return;
            }

            // (Option) Kiểm tra overlap khoảng thời gian để cảnh báo (không chặn cứng)
            boolean overlaps = tariffRepository.findAll().stream().anyMatch(t ->
                    t.getConnectorType().getConnectorTypeId().equals(connector.getConnectorTypeId())
                            && !t.getEffectiveTo().isBefore(effectiveFrom)
                            && !t.getEffectiveFrom().isAfter(effectiveTo)
            );
            if (overlaps) {
                log.warn("Tariff time range overlaps existing tariffs for {}. Consider splitting ranges.", connectorCode);
            }

            Tariff tariff = Tariff.builder()
                    .connectorType(connector)
                    .pricePerKWh(pricePerKWh)
                    .pricePerMin(pricePerMin)
                    .currency(currency)
                    .effectiveFrom(effectiveFrom)
                    .effectiveTo(effectiveTo)
                    .build();

            tariffRepository.save(tariff);
            log.info("Created Tariff: code={}, price={} {}, from={} to={}",
                    connectorCode, pricePerKWh, currency, effectiveFrom, effectiveTo);
        } catch (Exception e) {
            log.warn("Failed to create tariff for {}: {}", connectorCode, e.getMessage());
        }
    }

    // ================== PAYMENT METHODS ==================
    private void initPaymentMethods() {
        createPaymentMethodIfNotExists(
                PaymentType.EWALLET,
                PaymentProvider.VNPAY,
                "411111******1111",
                LocalDate.of(2030,10,28));

        createPaymentMethodIfNotExists(
                PaymentType.CASH,
                PaymentProvider.EVM,
                "CASH-0001",
                LocalDate.of(2030,10,28));
    }

    private void createPaymentMethodIfNotExists(PaymentType methodType,
                                                PaymentProvider provider,
                                                String accountNo,
                                                LocalDate expiryDate) {
        try {
            boolean exists = paymentMethodRepository
                    .existsByMethodTypeAndProviderAndAccountNo(methodType, provider, accountNo);
            if (exists) {
                log.info("PaymentMethod already exists (type={}, provider={}, accountNo={})",
                        methodType, provider, accountNo);
                return;
            }

            var pm = PaymentMethod.builder()
                    .methodType(methodType)
                    .provider(provider)
                    .accountNo(accountNo)
                    .expiryDate(expiryDate)
                    .build();

            paymentMethodRepository.save(pm);
            log.info("Created PaymentMethod (type={}, provider={}, accountNo={})",
                    methodType, provider, accountNo);
        } catch (Exception e) {
            log.warn("Failed to create PaymentMethod ({} / {} / {}): {}",
                    methodType, provider, accountNo, e.getMessage());
        }
    }

    // ================== DEFAULT BANNED DRIVER VIOLATIONS ==================
    private void initBannedDriverViolations() {
        try {
            // Danh sách số điện thoại 3 driver bị ban
            String[] bannedPhones = {
                    "0902914788",
                    "0321456987"
            };

            for (String phone : bannedPhones) {
                var driverOpt = driverRepository.findAll().stream()
                        .filter(d -> d.getUser() != null && phone.equals(d.getUser().getPhoneNumber()))
                        .findFirst();

                if (driverOpt.isEmpty()) {
                    log.warn("Driver with phone {} not found, skip violation seeding", phone);
                    continue;
                }

                var driver = driverOpt.get();

                // Tạo 3 vi phạm mẫu cho mỗi driver (sẽ tạo 1 Triplet tự động)
                for (int i = 1; i <= 3; i++) {
                    if (driverViolationRepository.existsByDriver_DriverIdAndDescriptionContaining(
                            driver.getDriverId(), "AutoBan Violation " + i)) {
                        log.info("Violation #{} already exists for driver {}", i, driver.getDriverId());
                        continue;
                    }

                    DriverViolation v = DriverViolation.builder()
                            .driver(driver)
                            .status(ViolationStatus.INACTIVE)
                            .description("AutoBan Violation " + i + " (no-show / misuse)")
                            .occurredAt(LocalDateTime.now().minusDays(3 - i))
                            .penaltyAmount(50000.0 * i)
                            .build();
                    v = driverViolationRepository.save(v);
                    log.info("Created AutoBan Violation {} for driver {}", v.getViolationId(), driver.getDriverId());

                    attachViolationToTriplet(driver, v);
                }

                // Cập nhật status driver sang BANNED nếu chưa
                if (driver.getStatus() != DriverStatus.BANNED) {
                    driver.setStatus(DriverStatus.BANNED);
                    driver.setLastActiveAt(LocalDateTime.now());
                    driverRepository.save(driver);
                    log.warn("Updated driver {} ({}) to BANNED due to seeded violations",
                            driver.getDriverId(), driver.getUser().getPhoneNumber());
                }
            }

            log.info("✅ Seeded violations and triplets for banned drivers.");

        } catch (Exception e) {
            log.error("❌ Failed to seed banned driver violations: {}", e.getMessage(), e);
        }
    }

    private void attachViolationToTriplet(Driver driver, DriverViolation violation) {
        var tripletOpt = driverViolationTripletRepository.findOpenByDriver(driver.getDriverId())
                .stream().findFirst();

        DriverViolationTriplet triplet = tripletOpt.orElseGet(() -> {
            var t = DriverViolationTriplet.builder()
                    .driver(driver)
                    .status(TripletStatus.IN_PROGRESS)
                    .countInGroup(0)
                    .totalPenalty(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            return driverViolationTripletRepository.save(t);
        });

        if (triplet.getCountInGroup() == 0) {
            triplet.setV1(violation);
            triplet.setWindowStartAt(violation.getOccurredAt());
        } else if (triplet.getCountInGroup() == 1) {
            triplet.setV2(violation);
        } else {
            triplet.setV3(violation);
        }

        triplet.setCountInGroup(triplet.getCountInGroup() + 1);
        triplet.setTotalPenalty(triplet.getTotalPenalty() + violation.getPenaltyAmount());

        if (triplet.getCountInGroup() >= 3) {
            triplet.setStatus(TripletStatus.OPEN);
            triplet.setWindowEndAt(violation.getOccurredAt());
            triplet.setClosedAt(LocalDateTime.now());
        }

        driverViolationTripletRepository.save(triplet);
    }

    // ================== SEED 2 VIOLATIONS CHO 0987456321 ==================
    private void initTwoViolationsFor0987456321() {
        final String phone = "0987456321";
        try {
            var driverOpt = driverRepository.findAll().stream()
                    .filter(d -> d.getUser() != null && phone.equals(d.getUser().getPhoneNumber()))
                    .findFirst();

            if (driverOpt.isEmpty()) {
                log.warn("Driver with phone {} not found, skip seeding 2 violations", phone);
                return;
            }

            var driver = driverOpt.get();

            // Tạo đúng 2 violation (không trùng)
            createViolationIfNotExists(driver, "Seed Violation A (two-only)", 75_000, 2);
            createViolationIfNotExists(driver, "Seed Violation B (two-only)", 95_000, 1);

            log.info("✅ Seeded exactly 2 violations for phone {}", phone);

        } catch (Exception e) {
            log.error("❌ Failed to seed 2 violations for {}: {}", phone, e.getMessage(), e);
        }
    }

    private void createViolationIfNotExists(Driver driver, String desc, double penalty, int daysAgo) {
        if (driverViolationRepository.existsByDriver_DriverIdAndDescriptionContaining(
                driver.getDriverId(), desc)) {
            log.info("Violation already exists for driver {} desc contains '{}'", driver.getDriverId(), desc);
            return;
        }

        DriverViolation v = DriverViolation.builder()
                .driver(driver)
                .status(ViolationStatus.ACTIVE) // hoặc INACTIVE tùy bạn muốn hiển thị
                .description(desc)
                .occurredAt(LocalDateTime.now().minusDays(daysAgo))
                .penaltyAmount(penalty)
                .build();

        v = driverViolationRepository.save(v);
//        attachViolationToTriplet(driver, v);
        log.info("Created violation {} for driver {}", v.getViolationId(), driver.getDriverId());
    }

    // ================== POLICIES (BR-01 ... BR-22) ==================
    private void initPolicies() {
        createPolicyIfNotExists(
                "BR-01",
                "Nếu người dùng nhập sai mật khẩu 3 lần liên tiếp, tài khoản sẽ bị khóa đăng nhập trong 3 phút."
        );
        createPolicyIfNotExists(
                "BR-02",
                "Khi thực hiện đặt chỗ (booking), tài xế phải chọn đúng một phương tiện từ danh sách xe của họ."
        );
        createPolicyIfNotExists(
                "BR-03",
                "Khi thêm xe, tài xế chỉ có thể chọn một model xe được hệ thống hỗ trợ."
        );
        createPolicyIfNotExists(
                "BR-04",
                "Một booking chỉ có thể được hủy nếu yêu cầu hủy xảy ra ít nhất 30 phút trước giờ bắt đầu sạc đã đặt."
        );
        createPolicyIfNotExists(
                "BR-05",
                "Tài xế bị cấm vì \"no-show\" lặp lại. \"No-show\" được định nghĩa là không bắt đầu phiên sạc trong vòng 30 phút kể từ thời gian bắt đầu booking. Nếu tài xế tích lũy 3 lần no-show (tính vĩnh viễn), trạng thái tài khoản của họ được đặt thành BANNED (BỊ CẤM)."
        );
        createPolicyIfNotExists(
                "BR-06",
                "Để gỡ bỏ lệnh cấm, người dùng phải trả một khoản phí phạt. Phí phạt được tính bằng: (tổng thời gian slot bị lãng phí - tính bằng phút) x (đơn giá phạt mỗi phút) đã được cấu hình cho cổng sạc cụ thể đó."
        );
        createPolicyIfNotExists(
                "BR-07",
                "Tổng phí được tính bằng tổng của Phí Năng lượng (Energy Charge) dựa trên kWh tiêu thụ và Phí Thời gian Lãng phí (Wasted Time Fee). Phí Thời gian Lãng phí = (Thời gian slot đã mua - Thời gian sạc thực tế) x (Đơn giá sạc mỗi phút)."
        );
        createPolicyIfNotExists(
                "BR-08",
                "Một Nhân viên (Staff) chỉ được phân công làm việc tại một trạm duy nhất tại bất kỳ thời điểm nào."
        );
        createPolicyIfNotExists(
                "BR-09",
                "Trong khi tài khoản bị BANNED, người dùng không thể thực hiện bất kỳ booking nào."
        );
        createPolicyIfNotExists(
                "BR-10",
                "Chỉ những người dùng đã xác thực và có vai trò \"Driver\" mới có thể thực hiện hành động đặt chỗ (booking)."
        );
        createPolicyIfNotExists(
                "BR-11",
                "Khi một Admin thêm thành viên Staff, Admin sẽ tạo tài khoản Staff và cung cấp thông tin đăng nhập để sử dụng."
        );
        createPolicyIfNotExists(
                "BR-12",
                "Thanh toán được yêu cầu ngay lập tức sau khi một phiên sạc kết thúc hoặc khi người dùng yêu cầu gỡ bỏ lệnh cấm; không hỗ trợ trả sau trong những trường hợp này."
        );
        createPolicyIfNotExists(
                "BR-13",
                "Một booking có thể bao gồm nhiều slot thời gian liên tiếp liền kề và trên cùng một đầu nối (connector), tối đa là 3 slot. Độ dài slot mặc định là 60 phút và có thể cấu hình."
        );
        createPolicyIfNotExists(
                "BR-14",
                "Một số điện thoại phải là duy nhất và không thể được sử dụng cho nhiều tài khoản."
        );
        createPolicyIfNotExists(
                "BR-15",
                "Tài khoản Staff không thể tự chỉnh sửa thông tin hồ sơ cá nhân. Họ chỉ được phép sử dụng chức năng \"Đổi Mật khẩu\"."
        );
        createPolicyIfNotExists(
                "BR-16",
                "Tương tự như Staff, tài khoản Admin cũng không thể tự chỉnh sửa thông tin hồ sơ cá nhân. Họ chỉ được phép sử dụng chức năng \"Đổi Mật khẩu\"."
        );
        createPolicyIfNotExists(
                "BR-17",
                "Mỗi cổng sạc chỉ có thể có một cấu hình giá hoạt động duy nhất tại bất kỳ thời điểm nào. Cấu hình này định nghĩa các đơn giá cho cổng đó."
        );
        createPolicyIfNotExists(
                "BR-18",
                "Bất kỳ chỉnh sửa nào đối với cấu hình giá sẽ có hiệu lực ngay lập tức. Mọi tính toán sau đó đều phải sử dụng đơn giá đã cập nhật."
        );
        createPolicyIfNotExists(
                "BR-19",
                "Để gỡ bỏ lệnh cấm (và thanh toán phí phạt theo BR-06), tài xế phải trực tiếp đến trạm sạc gần nhất và yêu cầu Nhân viên (Staff) tại chỗ hỗ trợ. Nhân viên sẽ thực hiện quy trình gỡ ban."
        );
        createPolicyIfNotExists(
                "BR-20",
                "Biển số xe phải là duy nhất trong toàn bộ hệ thống (nó không thể được đăng ký bởi hai tài xế khác nhau)."
        );
        createPolicyIfNotExists(
                "BR-21",
                "Một phương tiện không thể được ngưng hoạt động khỏi hồ sơ của Tài xế nếu nó được liên kết với một booking đang hoạt động hoặc sắp diễn ra (trong tương lai)."
        );
        createPolicyIfNotExists(
                "BR-22",
                "Khi một phiên sạc bị Nhân viên dừng thủ công, phí phạt đậu xe nhàn rỗi sẽ được miễn cho phiên sạc đó. Tài xế sẽ không bị tính phí phạt."
        );
    }

    private void createPolicyIfNotExists(String policyName, String description) {
        try {
            if (policyRepository.existsByPolicyName(policyName)) {
                log.info("Policy {} already exists, skip seeding", policyName);
                return;
            }

            Policy p = Policy.builder()
                    .policyName(policyName)
                    .policyDescription(description)
                    .build();

            policyRepository.save(p);
            log.info("Created policy {}: {}", policyName, description);
        } catch (Exception e) {
            log.warn("Failed to create policy {}: {}", policyName, e.getMessage());
        }
    }
}
