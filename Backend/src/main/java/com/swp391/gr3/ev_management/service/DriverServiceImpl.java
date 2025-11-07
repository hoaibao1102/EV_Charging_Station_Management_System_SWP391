package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.AddVehicleRequest;
import com.swp391.gr3.ev_management.dto.request.DriverRequest;
import com.swp391.gr3.ev_management.dto.request.DriverUpdateRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateVehicleRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingSessionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.DriverResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.VehicleResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.UserVehicleStatus;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.DriverDataMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.swp391.gr3.ev_management.mapper.DriverMapper;

@Service // Đánh dấu class là Spring Service xử lý nghiệp vụ liên quan đến Driver
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI)
@Slf4j // Cung cấp logger
public class DriverServiceImpl implements DriverService {

    // ====== Dependencies ======
    private final DriverRepository driverRepository;                 // Truy vấn/lưu Driver
    private final UserRepository userRepository;                     // Truy vấn/lưu User
    private final UserVehicleRepository userVehicleRepository;       // Truy vấn/lưu UserVehicle
    private final VehicleModelRepository vehicleModelRepository;     // Truy vấn VehicleModel
    private final DriverMapper driverMapper;                         // Map Entity <-> DTO cho Driver/Vehicle
    private final PasswordEncoder passwordEncoder;                   // Mã hoá/so khớp mật khẩu
    private final ChargingSessionRepository chargingSessionRepository; // Truy vấn ChargingSession của driver
    private final TransactionRepository transactionRepository;       // Truy vấn Transaction của driver

    /**
     * Tạo driver profile cho 1 user (nâng cấp user thành tài xế).
     * - Kiểm tra user tồn tại
     * - Kiểm tra không bị trùng driver theo userId
     * - Gán trạng thái (mặc định ACTIVE nếu request null)
     * - Lưu Driver và trả về DTO
     */
    @Override
    @Transactional
    public DriverResponse createDriverProfile(Long userId, DriverRequest request) {
        log.info("Creating driver for user {}", userId);

        // Tìm user theo userId; nếu không có -> lỗi
        User user = userRepository.findUserByUserId(userId);
        if (user == null) {
            throw new ErrorException("User not found with ID: " + userId);
        }

        // Kiểm tra trùng driver theo userId (1 user chỉ có 1 driver)
        if (driverRepository.existsByUser_UserId(userId)) {
            throw new ConflictException("Driver already exists for userId " + userId);
        }

        // Khởi tạo driver với status lấy từ request hoặc mặc định ACTIVE
        Driver driver = new Driver();
        driver.setUser(user);
        DriverStatus status = (request != null && request.getDriverStatus() != null)
                ? request.getDriverStatus()
                : DriverStatus.ACTIVE;
        driver.setStatus(status);

        try {
            // Lưu driver
            Driver saved = driverRepository.save(driver);
            // Trả về DTO
            return driverMapper.toDriverResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Trường hợp race-condition: unique constraint cho UserID vi phạm
            throw new ConflictException("Driver already exists for userId " + userId);
        }
    }

    /**
     * Lấy driver theo userId (dùng cho self-profile qua token).
     * - Join fetch User để có đủ thông tin
     * - Map sang DTO trả về
     */
    @Override
    @Transactional(readOnly = true)
    public DriverResponse getByUserId(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));
        return driverMapper.toDriverResponse(driver);
    }

    /**
     * Lấy toàn bộ danh sách driver (admin xem).
     */
    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(driverMapper::toDriverResponse)
                .toList();
    }

    /**
     * Driver tự cập nhật hồ sơ (userId lấy từ token).
     * - Tìm driver theo userId
     * - Cập nhật các field của User (nếu có)
     * - Đặt status ACTIVE (theo nghiệp vụ)
     * - Lưu & trả DTO
     */
    @Override
    @Transactional
    public DriverResponse updateDriverProfile(Long userId, DriverUpdateRequest req) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        User user = driver.getUser();
        // Cập nhật các trường nếu request cung cấp
        if (req.getName() != null)        user.setName(req.getName());
        if (req.getEmail() != null)       user.setEmail(req.getEmail());
        if (req.getAddress() != null)     user.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null)      user.setGender(req.getGender());
        driver.setStatus(DriverStatus.ACTIVE); // Có thể cố định ACTIVE khi update profile

        Driver updated = driverRepository.save(driver);
        return driverMapper.toDriverResponse(updated);
    }

    /**
     * Driver tự đổi mật khẩu.
     * - Kiểm tra driver & khớp mật khẩu cũ
     * - Kiểm tra new == confirm, độ dài >= 6, không trùng mật khẩu cũ
     * - Mã hoá & lưu
     */
    @Override
    @Transactional
    public DriverResponse updateDriverPassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));
        User user = driver.getUser();

        // So khớp mật khẩu cũ
        String currentHash = user.getPasswordHash();
        if (!passwordEncoder.matches(oldPassword, currentHash)) {
            throw new ConflictException("Old password is incorrect");
        }
        // new == confirm
        if (!newPassword.equals(confirmNewPassword)) {
            throw new ConflictException("New password and confirm password do not match");
        }
        // Độ dài tối thiểu
        if (newPassword == null || newPassword.length() < 6) {
            throw new ConflictException("New password must be at least 6 characters");
        }
        // Không cho trùng mật khẩu cũ
        if (passwordEncoder.matches(newPassword, currentHash)) {
            throw new ConflictException("New password must be different from old password");
        }

        // Mã hoá và lưu
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return driverMapper.toDriverResponse(driver);
    }

    /**
     * Admin cập nhật trạng thái driver theo userId.
     * - Tìm driver
     * - Set status mới
     * - Lưu & trả DTO
     */
    @Override
    @Transactional
    public DriverResponse updateStatus(Long userId, DriverStatus status) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));
        driver.setStatus(status);
        driverRepository.save(driver);
        return driverMapper.toDriverResponse(driver);
    }

    // =================== UC-04: VEHICLE MANAGEMENT ===================

    /**
     * Driver thêm xe vào hồ sơ (BR-02).
     * Ràng buộc (BR-03):
     * - VehicleModel phải tồn tại và ở trạng thái ACTIVE
     * - Biển số không được trùng (so sánh theo normalized)
     * - Format biển số theo chuẩn VN trước khi lưu (đẹp & nhất quán)
     */
    @Override
    @Transactional
    public VehicleResponse addVehicle(Long userId, AddVehicleRequest request) {
        log.info("Adding vehicle for userId: {}, licensePlate: {}", userId, request.getLicensePlate());

        // Tìm driver
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // Kiểm tra model tồn tại
        VehicleModel vehicleModel = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ErrorException("Vehicle model not found with ID: " + request.getModelId()));

        // Chỉ cho phép model ACTIVE
        if (vehicleModel.getStatus() != VehicleModelStatus.ACTIVE) {
            throw new ConflictException("Cannot add vehicle: vehicle model is not ACTIVE");
        }

        // Chuẩn hoá biển số để so uniqueness (loại bỏ khoảng trắng, dấu gạch, chấm; upper-case)
        String normalizedPlate = normalizePlate(request.getLicensePlate());

        // Kiểm tra trùng biển số: normalize tất cả rồi so sánh
        List<UserVehicle> existingVehicles = userVehicleRepository.findAll();
        for (UserVehicle v : existingVehicles) {
            if (normalizePlate(v.getVehiclePlate()).equals(normalizedPlate)) {
                throw new ConflictException("License plate already registered: " + request.getLicensePlate());
            }
        }

        // Format biển số theo chuẩn VN trước khi lưu (ví dụ: 86B381052 -> 86B-381.05)
        String formattedPlate = formatVietnamPlate(request.getLicensePlate());

        // Tạo entity UserVehicle
        UserVehicle vehicle = UserVehicle.builder()
                .driver(driver)
                .vehiclePlate(formattedPlate) // Lưu dạng đã format
                .model(vehicleModel)
                .status(UserVehicleStatus.ACTIVE)
                .build();

        // Lưu & map response
        UserVehicle saved = userVehicleRepository.save(vehicle);
        log.info("Vehicle added successfully: {}", saved.getVehicleId());

        return driverMapper.toVehicleResponse(saved);
    }

    // Helper: Chuẩn hoá biển số để so sánh uniqueness
    // - trim + upper-case + bỏ khoảng trắng, dấu '.' và '-'
    private String normalizePlate(String plate) {
        if (plate == null) return null;
        String trimmed = plate.trim().toUpperCase();
        // Giữ lại chữ & số, bỏ ký tự phân cách
        return trimmed.replaceAll("[ .-]", "");
    }

    /**
     * Helper: Format biển số theo mẫu phổ biến VN
     * Ví dụ: 86B381052 → 86B-381.05; 30G12345 → 30G-123.45
     * Quy ước đơn giản: [2 số][1-2 chữ]-[3 số].[2 số] nếu đủ dữ liệu
     * Nếu không match, trả về normalized.
     */
    private String formatVietnamPlate(String plate) {
        if (plate == null || plate.isEmpty()) return plate;

        String normalized = normalizePlate(plate);

        // Heuristic: 2 số + 1-2 chữ + 4-5 số
        if (normalized.length() >= 8 && normalized.length() <= 10) {
            String prefix = normalized.substring(0, 2); // 2 số đầu
            String letters = ""; // 1-2 chữ
            String numbers;      // phần số phía sau

            int i = 2;
            // Lấy 1-2 ký tự chữ
            while (i < normalized.length() && Character.isLetter(normalized.charAt(i))) {
                letters += normalized.charAt(i);
                i++;
            }
            // Phần còn lại là số
            numbers = normalized.substring(i);

            // Tạo định dạng A-BBB.CC nếu đủ 5 số; nếu >2 số ở phần cuối thì cắt 2
            if (numbers.length() >= 4) {
                String part1 = numbers.substring(0, 3);
                String part2 = numbers.substring(3);
                if (part2.length() > 2) {
                    part2 = part2.substring(0, 2);
                }
                return prefix + letters + "-" + part1 + "." + part2;
            }
        }

        // Không match -> trả về normalized (đã upper-case, bỏ phân cách)
        return normalized;
    }

    /**
     * Lấy danh sách xe của driver.
     * - Tìm driver theo userId
     * - Lấy list xe chi tiết (deep graph)
     * - Map sang DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(Long userId) {
        log.info("Getting vehicles for userId: {}", userId);

        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        List<UserVehicle> vehicles = userVehicleRepository.findByDriverIdWithDetails(driver.getDriverId());

        return vehicles.stream()
                .map(driverMapper::toVehicleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin xe (model, license plate) của driver.
     * - Kiểm tra quyền sở hữu
     * - Nếu đổi model: model phải ACTIVE
     * - Nếu đổi biển số: kiểm tra trùng theo normalized, sau đó format VN rồi lưu
     */
    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long userId, Long vehicleId, UpdateVehicleRequest request) {
        log.info("Updating vehicle {} for userId: {}", vehicleId, userId);

        // 1) Xác thực driver
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2) Lấy vehicle
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ErrorException("Vehicle not found with ID: " + vehicleId));

        // 3) Kiểm tra quyền sở hữu
        if (!vehicle.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new ConflictException("Vehicle does not belong to this driver");
        }

        // 4) Cập nhật model nếu có đổi và model tồn tại/ACTIVE
        if (request.getModelId() != null && !request.getModelId().equals(
                vehicle.getModel() != null ? vehicle.getModel().getModelId() : null)) {
            VehicleModel newModel = vehicleModelRepository.findById(request.getModelId())
                    .orElseThrow(() -> new ErrorException("Vehicle model not found with ID: " + request.getModelId()));

            if (newModel.getStatus() != VehicleModelStatus.ACTIVE) {
                throw new ConflictException("Cannot update vehicle: target vehicle model is not ACTIVE");
            }

            vehicle.setModel(newModel);
        }

        // 5) Cập nhật biển số nếu có thay đổi
        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            String normalizedNew = normalizePlate(request.getLicensePlate());

            // Kiểm tra trùng (loại trừ chính vehicle hiện tại)
            List<UserVehicle> existingVehicles = userVehicleRepository.findAll();
            for (UserVehicle v : existingVehicles) {
                if (!v.getVehicleId().equals(vehicleId)) {
                    if (normalizePlate(v.getVehiclePlate()).equals(normalizedNew)) {
                        throw new ConflictException("License plate already registered: " + request.getLicensePlate());
                    }
                }
            }

            // Format và set
            String formatted = formatVietnamPlate(request.getLicensePlate());
            vehicle.setVehiclePlate(formatted);
        }

        // Lưu & trả response
        UserVehicle saved = userVehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully: {}", saved.getVehicleId());
        return driverMapper.toVehicleResponse(saved);
    }

    /**
     * Cập nhật trạng thái xe (ACTIVE/INACTIVE...) của driver.
     * - Kiểm tra driver & quyền sở hữu
     * - Không đổi thì trả luôn
     * - Lưu và trả DTO
     */
    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long userId, Long vehicleId, UserVehicleStatus status) {
        log.info("Updating vehicle {} status for userId: {} -> {}", vehicleId, userId, status);

        if (status == null) {
            throw new ErrorException("Status must not be null");
        }

        // 1) Xác thực driver
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2) Lấy vehicle
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ErrorException("Vehicle not found with ID: " + vehicleId));

        // 3) Kiểm tra quyền sở hữu
        if (!vehicle.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new ConflictException("Vehicle does not belong to this driver");
        }

        // (Optional) Rule thêm: nếu đang có phiên sạc thì không cho INACTIVE (tuỳ nghiệp vụ)

        // 4) Không thay đổi gì -> trả về ngay
        if (vehicle.getStatus() == status) {
            return driverMapper.toVehicleResponse(vehicle);
        }

        // 5) Cập nhật & lưu
        UserVehicleStatus old = vehicle.getStatus();
        vehicle.setStatus(status);
        UserVehicle saved = userVehicleRepository.save(vehicle);

        log.info("Vehicle status updated: {} -> {}", old, status);
        return driverMapper.toVehicleResponse(saved);
    }

    /**
     * Lấy lịch sử giao dịch (transactions) của driver theo userId.
     * - Tìm driver (xác thực tồn tại)
     * - Truy vấn transactions (deep graph)
     * - Map sang DTO tóm tắt
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionBriefResponse> getMyTransactions(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        List<Transaction> txs = transactionRepository.findAllDeepGraphByDriverUserId(userId);
        return DriverDataMapper.toTransactionBriefResponseList(txs);
    }

    /**
     * Lấy lịch sử phiên sạc của driver theo userId.
     * - Tìm driver (xác thực tồn tại)
     * - Truy vấn sessions (deep graph)
     * - Map sang DTO tóm tắt
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChargingSessionBriefResponse> getMyChargingSessions(Long userId) {
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        List<ChargingSession> sessions = chargingSessionRepository.findAllByDriverUserIdDeep(userId);
        return DriverDataMapper.toChargingSessionBriefResponseList(sessions);
    }

}
