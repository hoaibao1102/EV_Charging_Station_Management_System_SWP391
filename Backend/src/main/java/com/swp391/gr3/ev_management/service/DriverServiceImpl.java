package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.DTO.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.DTO.response.DriverResponse;
import com.swp391.gr3.ev_management.DTO.response.VehicleResponse;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.entity.Driver;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.DriverRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import com.swp391.gr3.ev_management.repository.UserVehicleRepository;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.swp391.gr3.ev_management.mapper.DriverMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final DriverMapper driverMapper;

    /**
     * Tạo driver profile cho 1 user (nâng cấp thành Driver)
     */
    @Override
    @Transactional
    public DriverResponse createDriverProfile(Long userId, DriverRequest request) {
        log.info("Creating driver for user {}", userId);

        User user = userRepository.findUserByUserId(userId);
        if (user == null) {
            throw new NotFoundException("User not found with ID: " + userId);
        }

        // Check trùng đúng cách theo userId
        if (driverRepository.existsByUser_UserId(userId)) {
            throw new ConflictException("Driver already exists for userId " + userId);
        }

        Driver driver = new Driver();
        driver.setUser(user);
        DriverStatus status = (request != null && request.getDriverStatus() != null)
                ? request.getDriverStatus()
                : DriverStatus.ACTIVE;
        driver.setStatus(status);

        try {
            Driver saved = driverRepository.save(driver);
            return driverMapper.toDriverResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Phòng race condition: nếu unique(UserID) ở DB bắn lỗi
            throw new ConflictException("Driver already exists for userId " + userId);
        }
    }

    /**
     * Lấy driver theo userId (dùng cho màn self-profile qua token)
     */
    @Override
    @Transactional(readOnly = true)
    public DriverResponse getByUserId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));
    return driverMapper.toDriverResponse(driver);
    }

    /**
     * Lấy driver theo driverId (dùng khi admin thao tác theo PK driver)
     */
    @Override
    @Transactional(readOnly = true)
    public DriverResponse getByDriverId(Long driverId) {
        Driver driver = driverRepository.findByDriverIdWithUser(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with driverId " + driverId));
    return driverMapper.toDriverResponse(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
    return driverRepository.findAll()
        .stream()
        .map(driverMapper::toDriverResponse)
        .toList();
    }

    /**
     * Driver tự cập nhật hồ sơ — userId lấy từ token
     */
    @Override
    @Transactional
    public DriverResponse updateDriverProfile(Long userId, DriverUpdateRequest req) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        User user = driver.getUser();
        if (req.getName() != null)        user.setName(req.getName());
        if (req.getEmail() != null)       user.setEmail(req.getEmail());
        if (req.getAddress() != null)     user.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        driver.setStatus(DriverStatus.ACTIVE);

    Driver updated = driverRepository.save(driver);
    return driverMapper.toDriverResponse(updated);
    }

    /**
     * Admin đổi trạng thái driver theo userId (đúng với flow controller của bạn)
     */
    @Override
    @Transactional
    public DriverResponse updateStatus(Long userId, DriverStatus status) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));
        driver.setStatus(status);
    driverRepository.save(driver);
    return driverMapper.toDriverResponse(driver);
    }

    /**
     * Lấy driver profile theo driverId (nếu bạn cần riêng)
     */
    @Transactional(readOnly = true)
    public DriverResponse getDriverProfile(Long driverId) {
        Driver driver = driverRepository.findByDriverIdWithUser(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with driverId: " + driverId));
    return driverMapper.toDriverResponse(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByStatus(DriverStatus status) {
    return driverRepository.findByStatus(status)
        .stream()
        .map(driverMapper::toDriverResponse)
        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByName(String name) {
    return driverRepository.findByUser_NameContainingIgnoreCase(name)
        .stream()
        .map(driverMapper::toDriverResponse)
        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByPhoneNumber(String phoneNumber) {
    return driverRepository.findByUser_PhoneNumberContaining(phoneNumber)
        .stream()
        .map(driverMapper::toDriverResponse)
        .toList();
    }

    // =================== UC-04: VEHICLE MANAGEMENT ===================

    /**
     * BR-02: Driver thêm xe vào hồ sơ của mình
     * BR-03: Kiểm tra VehicleModel tồn tại và license plate chưa được đăng ký
     */
    @Override
    @Transactional
    public VehicleResponse addVehicle(Long userId, AddVehicleRequest request) {
        log.info("Adding vehicle for userId: {}, licensePlate: {}", userId, request.getLicensePlate());

        // Lấy driver từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        // BR-03: Kiểm tra VehicleModel có tồn tại
        VehicleModel vehicleModel = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new NotFoundException("Vehicle model not found with ID: " + request.getModelId()));

        // Chuẩn hoá biển số để kiểm tra trùng (chỉ chữ + số, bỏ hết ký tự đặc biệt)
        String normalizedPlate = normalizePlate(request.getLicensePlate());

        // Kiểm tra license plate đã tồn tại chưa (so sánh theo dạng normalized)
        // Vì DB lưu có format, nên ta cần check bằng cách normalize cả DB records
        List<UserVehicle> existingVehicles = userVehicleRepository.findAll();
        for (UserVehicle v : existingVehicles) {
            if (normalizePlate(v.getVehiclePlate()).equals(normalizedPlate)) {
                throw new ConflictException("License plate already registered: " + request.getLicensePlate());
            }
        }

        // Format biển số theo chuẩn VN trước khi lưu: 86B381052 → 86B-381.05
        String formattedPlate = formatVietnamPlate(request.getLicensePlate());

        // Tạo UserVehicle mới
        UserVehicle vehicle = UserVehicle.builder()
                .driver(driver)
                .vehiclePlate(formattedPlate) // Lưu dạng có format đẹp
                .model(vehicleModel)
                .build();

        UserVehicle saved = userVehicleRepository.save(vehicle);
        log.info("Vehicle added successfully: {}", saved.getVehicleId());

    return driverMapper.toVehicleResponse(saved);
    }

    // Chuẩn hoá: trim -> upper-case -> loại bỏ khoảng trắng, dấu '-' và '.' để so sánh uniqueness
    private String normalizePlate(String plate) {
        if (plate == null) return null;
        String trimmed = plate.trim().toUpperCase();
        // Giữ lại chữ và số để tránh khác biệt do ký tự phân cách
        return trimmed.replaceAll("[ .-]", "");
    }

    /**
     * Format biển số theo chuẩn VN: 86B381052 → 86B-381.05 hoặc 30G12345 → 30G-123.45
     * Format: [2 số][1-2 chữ]-[3 số].[2 số]
     */
    private String formatVietnamPlate(String plate) {
        if (plate == null || plate.isEmpty()) return plate;

        String normalized = normalizePlate(plate);

        // Phát hiện pattern: 2 số + 1-2 chữ + 4-5 số
        // VD: 86B381052 (9 ký tự) hoặc 30AB12345 (10 ký tự)
        if (normalized.length() >= 8 && normalized.length() <= 10) {
            // Tách: [2 số đầu][chữ cái][số còn lại]
            String prefix = normalized.substring(0, 2); // 86
            String letters = ""; // B hoặc AB
            String numbers = ""; // 381052

            int i = 2;
            // Lấy chữ cái (1-2 chữ)
            while (i < normalized.length() && Character.isLetter(normalized.charAt(i))) {
                letters += normalized.charAt(i);
                i++;
            }
            // Phần còn lại là số
            numbers = normalized.substring(i);

            // Format: 86B-381.05 (nếu đủ 5 số) hoặc 30G-123.45 (nếu đủ 5 số)
            if (numbers.length() >= 4) {
                String part1 = numbers.substring(0, 3); // 381 hoặc 123
                String part2 = numbers.substring(3); // 052 hoặc 45
                // Lấy 2 số cuối cho part2
                if (part2.length() > 2) {
                    part2 = part2.substring(0, 2);
                }
                return prefix + letters + "-" + part1 + "." + part2;
            }
        }

        // Nếu không match format chuẩn, trả về normalized (uppercase, no space)
        return normalized;
    }

    /**
     * Lấy danh sách xe của driver
     */
    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(Long userId) {
        log.info("Getting vehicles for userId: {}", userId);

        // Lấy driver từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        // Lấy danh sách xe với thông tin chi tiết
        List<UserVehicle> vehicles = userVehicleRepository.findByDriverIdWithDetails(driver.getDriverId());

    return vehicles.stream()
        .map(driverMapper::toVehicleResponse)
        .collect(Collectors.toList());
    }

    /**
     * Xóa xe khỏi hồ sơ driver
     * BR-02: Chỉ được xóa xe thuộc về driver đang đăng nhập
     */
    //Todo: check nếu xe đang có lịch sạc thì không được xóa, soft delete, v.v.
    @Override
    @Transactional
    public void removeVehicle(Long userId, Long vehicleId) {
        log.info("Removing vehicle {} for userId: {}", vehicleId, userId);

        // Lấy driver từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Driver not found with userId " + userId));

        // Lấy vehicle
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with ID: " + vehicleId));

        // Kiểm tra xe có thuộc về driver này không
        if (!vehicle.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new ConflictException("Vehicle does not belong to this driver");
        }

        userVehicleRepository.delete(vehicle);
        log.info("Vehicle removed successfully: {}", vehicleId);
    }

}