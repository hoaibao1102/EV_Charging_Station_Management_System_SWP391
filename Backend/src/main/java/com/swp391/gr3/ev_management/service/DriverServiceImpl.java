package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.*;
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
import java.util.Optional;
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
    private final VehicleModelService vehicleModelService;           // Truy vấn VehicleModel
    private final DriverMapper driverMapper;                         // Map Entity <-> DTO cho Driver/Vehicle
    private final PasswordEncoder passwordEncoder;                   // Mã hoá/so khớp mật khẩu
    private final ChargingSessionService chargingSessionService;     // Truy vấn ChargingSession của driver
    private final TransactionService transactionService;             // Truy vấn Transaction của driver

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

        // 1️⃣ Tìm user theo userId; nếu không có -> ném ErrorException
        User user = userRepository.findUserByUserId(userId);
        if (user == null) {
            throw new ErrorException("User not found with ID: " + userId);
        }

        // 2️⃣ Kiểm tra xem user này đã có Driver chưa
        //    - 1 user chỉ được phép có 1 driver profile
        if (driverRepository.existsByUser_UserId(userId)) {
            throw new ConflictException("Driver already exists for userId " + userId);
        }

        // 3️⃣ Khởi tạo mới đối tượng Driver
        Driver driver = new Driver();
        driver.setUser(user); // gắn user với driver

        // 4️⃣ Xác định trạng thái driver:
        //    - Nếu request có truyền driverStatus -> dùng giá trị đó
        //    - Nếu không -> mặc định ACTIVE
        DriverStatus status = (request != null && request.getDriverStatus() != null)
                ? request.getDriverStatus()
                : DriverStatus.ACTIVE;
        driver.setStatus(status);

        try {
            // 5️⃣ Lưu driver xuống DB
            Driver saved = driverRepository.save(driver);
            // 6️⃣ Map sang DTO DriverResponse trả về cho client
            return driverMapper.toDriverResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // 7️⃣ Nếu gặp lỗi vi phạm ràng buộc (unique key userId...) do race-condition -> ném ConflictException
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
        // 1️⃣ Tìm driver theo userId, join fetch kèm User
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Map entity Driver -> DTO DriverResponse
        return driverMapper.toDriverResponse(driver);
    }

    /**
     * Lấy toàn bộ danh sách driver (admin xem).
     */
    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        // 1️⃣ Lấy tất cả Driver từ DB
        return driverRepository.findAll()
                .stream()
                // 2️⃣ Map từng driver sang DTO DriverResponse
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
        // 1️⃣ Lấy Driver kèm User từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Lấy User để update các thông tin cá nhân
        User user = driver.getUser();

        // 3️⃣ Cập nhật từng trường nếu request có truyền (không override khi null)
        if (req.getName() != null)        user.setName(req.getName());
        if (req.getEmail() != null)       user.setEmail(req.getEmail());
        if (req.getAddress() != null)     user.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null)      user.setGender(req.getGender());

        // 4️⃣ Theo business: khi driver update profile -> đảm bảo status ở ACTIVE
        driver.setStatus(DriverStatus.ACTIVE);

        // 5️⃣ Lưu driver (cascade có thể update luôn user nếu cấu hình)
        Driver updated = driverRepository.save(driver);

        // 6️⃣ Map sang DTO trả về
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
    public DriverResponse updateDriverPassword(Long userId, UpdatePasswordRequest request) {
        // 1️⃣ Lấy driver kèm user từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));
        User user = driver.getUser();

        // 2️⃣ Lấy hash password hiện tại từ User
        String currentHash = user.getPasswordHash();

        // 3️⃣ So khớp mật khẩu cũ (plain) với hash trong DB
        if (!passwordEncoder.matches(request.getOldPassword(), currentHash)) {
            throw new ConflictException("Old password is incorrect");
        }

        // 4️⃣ Kiểm tra newPassword & confirmNewPassword giống nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ConflictException("New password and confirm password do not match");
        }

        // 5️⃣ Đảm bảo mật khẩu mới không null và có độ dài tối thiểu 6 ký tự
        if (request.getNewPassword().length() < 6) {
            throw new ConflictException("New password must be at least 6 characters");
        }

        // 6️⃣ Không cho phép dùng lại mật khẩu cũ (so khớp newPassword với currentHash)
        if (passwordEncoder.matches(request.getNewPassword(), currentHash)) {
            throw new ConflictException("New password must be different from old password");
        }

        // 7️⃣ Encode mật khẩu mới và lưu vào user
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 8️⃣ Trả về DriverResponse (thông tin driver vẫn như cũ, chỉ thay đổi password)
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
        // 1️⃣ Lấy driver theo userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Cập nhật trạng thái (ACTIVE, INACTIVE, SUSPENDED, ...)
        driver.setStatus(status);

        // 3️⃣ Lưu driver
        driverRepository.save(driver);

        // 4️⃣ Map sang DTO trả về
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

        // 1️⃣ Tìm driver theo userId (kèm user) để xác định chủ xe
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Kiểm tra model xe có tồn tại không
        VehicleModel vehicleModel = vehicleModelService.findById(request.getModelId())
                .orElseThrow(() -> new ErrorException("Vehicle model not found with ID: " + request.getModelId()));

        // 3️⃣ Chỉ cho phép sử dụng VehicleModel ở trạng thái ACTIVE
        if (vehicleModel.getStatus() != VehicleModelStatus.ACTIVE) {
            throw new ConflictException("Cannot add vehicle: vehicle model is not ACTIVE");
        }

        // 4️⃣ Chuẩn hoá biển số (normalize) để phục vụ check uniqueness
        String normalizedPlate = normalizePlate(request.getLicensePlate());

        // 5️⃣ Với mọi xe đã tồn tại, normalize biển số rồi so sánh -> nếu trùng thì không cho thêm
        List<UserVehicle> existingVehicles = userVehicleRepository.findAll();
        for (UserVehicle v : existingVehicles) {
            if (normalizePlate(v.getVehiclePlate()).equals(normalizedPlate)) {
                throw new ConflictException("License plate already registered: " + request.getLicensePlate());
            }
        }

        // 6️⃣ Format biển số theo chuẩn Việt Nam trước khi lưu (để hiển thị đẹp, nhất quán)
        String formattedPlate = formatVietnamPlate(request.getLicensePlate());

        // 7️⃣ Tạo entity UserVehicle mới và gán driver, model, plate
        UserVehicle vehicle = UserVehicle.builder()
                .driver(driver)                   // gán driver sở hữu xe
                .vehiclePlate(formattedPlate)     // lưu biển số đã format
                .model(vehicleModel)              // gán VehicleModel
                .status(UserVehicleStatus.ACTIVE) // default trạng thái xe là ACTIVE
                .build();

        // 8️⃣ Lưu userVehicle xuống DB
        UserVehicle saved = userVehicleRepository.save(vehicle);
        log.info("Vehicle added successfully: {}", saved.getVehicleId());

        // 9️⃣ Map sang VehicleResponse để trả về cho client
        return driverMapper.toVehicleResponse(saved);
    }

    // Helper: Chuẩn hoá biển số để so sánh uniqueness
    // - trim + upper-case + bỏ khoảng trắng, dấu '.' và '-'
    private String normalizePlate(String plate) {
        if (plate == null) return null;
        String trimmed = plate.trim().toUpperCase();
        // Giữ lại chữ & số, bỏ các ký tự phân cách (space, chấm, gạch)
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

        // 1️⃣ Normalize trước (bỏ dấu, khoảng trắng, upper-case)
        String normalized = normalizePlate(plate);

        // 2️⃣ Áp dụng heuristic: độ dài từ 8 tới 10, dạng 2 số + 1-2 chữ + 4-5 số
        if (normalized.length() >= 8 && normalized.length() <= 10) {
            String prefix = normalized.substring(0, 2); // 2 số đầu (mã tỉnh)
            String letters = "";                        // 1-2 chữ tiếp theo (series)
            String numbers;                             // phần chữ số còn lại

            int i = 2;
            // Lấy 1-2 ký tự chữ cái sau prefix
            while (i < normalized.length() && Character.isLetter(normalized.charAt(i))) {
                letters += normalized.charAt(i);
                i++;
            }
            // Phần còn lại là số
            numbers = normalized.substring(i);

            // 3️⃣ Nếu còn đủ số phía sau (>=4), tiến hành tách thành BBB.CC
            if (numbers.length() >= 4) {
                String part1 = numbers.substring(0, 3); // 3 số đầu
                String part2 = numbers.substring(3);    // phần còn lại

                // Nếu part2 > 2 số -> cắt lấy 2 số đầu
                if (part2.length() > 2) {
                    part2 = part2.substring(0, 2);
                }
                // Tạo chuỗi dạng 86B-381.05
                return prefix + letters + "-" + part1 + "." + part2;
            }
        }

        // 4️⃣ Nếu không match được format -> trả về normalized (dù không có dấu gạch, chấm nhưng thống nhất)
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

        // 1️⃣ Tìm driver từ userId (xác thực driver tồn tại)
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Truy vấn danh sách UserVehicle gắn với driverId (kèm chi tiết model, v.v.)
        List<UserVehicle> vehicles = userVehicleRepository.findByDriverIdWithDetails(driver.getDriverId());

        // 3️⃣ Map danh sách entity -> DTO VehicleResponse
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

        // 1️⃣ Xác thực driver từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Lấy vehicle theo vehicleId
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ErrorException("Vehicle not found with ID: " + vehicleId));

        // 3️⃣ Kiểm tra quyền sở hữu xe: driver trong vehicle phải trùng với driver hiện tại
        if (!vehicle.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new ConflictException("Vehicle does not belong to this driver");
        }

        // 4️⃣ Cập nhật model nếu request có modelId khác với model hiện tại
        if (request.getModelId() != null && !request.getModelId().equals(
                vehicle.getModel() != null ? vehicle.getModel().getModelId() : null)) {

            // 4.1) Tìm model mới
            VehicleModel newModel = vehicleModelService.findById(request.getModelId())
                    .orElseThrow(() -> new ErrorException("Vehicle model not found with ID: " + request.getModelId()));

            // 4.2) Chỉ cho phép chuyển sang model ở trạng thái ACTIVE
            if (newModel.getStatus() != VehicleModelStatus.ACTIVE) {
                throw new ConflictException("Cannot update vehicle: target vehicle model is not ACTIVE");
            }

            // 4.3) Set model mới
            vehicle.setModel(newModel);
        }

        // 5️⃣ Cập nhật biển số nếu request có truyền và không rỗng
        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            // 5.1) Normalize biển số mới
            String normalizedNew = normalizePlate(request.getLicensePlate());

            // 5.2) Kiểm tra trùng với các vehicle khác (loại trừ chính vehicle hiện tại)
            List<UserVehicle> existingVehicles = userVehicleRepository.findAll();
            for (UserVehicle v : existingVehicles) {
                if (!v.getVehicleId().equals(vehicleId)) {
                    if (normalizePlate(v.getVehiclePlate()).equals(normalizedNew)) {
                        throw new ConflictException("License plate already registered: " + request.getLicensePlate());
                    }
                }
            }

            // 5.3) Nếu hợp lệ -> format theo chuẩn VN và set lại cho vehicle
            String formatted = formatVietnamPlate(request.getLicensePlate());
            vehicle.setVehiclePlate(formatted);
        }

        // 6️⃣ Lưu lại vehicle đã cập nhật
        UserVehicle saved = userVehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully: {}", saved.getVehicleId());

        // 7️⃣ Map sang DTO trả kết quả
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

        // 1️⃣ Validate tham số status
        if (status == null) {
            throw new ErrorException("Status must not be null");
        }

        // 2️⃣ Xác thực driver từ userId
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 3️⃣ Tìm vehicle theo vehicleId
        UserVehicle vehicle = userVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ErrorException("Vehicle not found with ID: " + vehicleId));

        // 4️⃣ Kiểm tra quyền sở hữu xe
        if (!vehicle.getDriver().getDriverId().equals(driver.getDriverId())) {
            throw new ConflictException("Vehicle does not belong to this driver");
        }

        // (Optional) Có thể kiểm tra nếu xe đang trong phiên sạc thì không cho INACTIVE...

        // 5️⃣ Nếu trạng thái mới giống trạng thái cũ -> không cần update, trả về luôn
        if (vehicle.getStatus() == status) {
            return driverMapper.toVehicleResponse(vehicle);
        }

        // 6️⃣ Cập nhật trạng thái và lưu
        UserVehicleStatus old = vehicle.getStatus();
        vehicle.setStatus(status);
        UserVehicle saved = userVehicleRepository.save(vehicle);

        log.info("Vehicle status updated: {} -> {}", old, status);

        // 7️⃣ Trả về DTO VehicleResponse
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
        // 1️⃣ Đảm bảo driver tồn tại (xác thực user là driver)
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Truy vấn tất cả Transaction liên quan đến driver (deep graph để tránh N+1)
        List<Transaction> txs = transactionService.findAllDeepGraphByDriverUserId(userId);

        // 3️⃣ Map list Transaction -> list TransactionBriefResponse (DTO tóm tắt)
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
        // 1️⃣ Đảm bảo driver tồn tại
        Driver driver = driverRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Driver not found with userId " + userId));

        // 2️⃣ Truy vấn tất cả các ChargingSession của driver (deep graph)
        List<ChargingSession> sessions = chargingSessionService.findAllByDriverUserIdDeep(userId);

        // 3️⃣ Map sang DTO tóm tắt để trả về cho client
        return DriverDataMapper.toChargingSessionBriefResponseList(sessions);
    }

    @Override
    public Optional<Driver> findByUser_UserId(Long userId) {
        // Tìm driver theo userId, trả về Optional để caller tự xử lý
        return driverRepository.findByUser_UserId(userId);
    }

    @Override
    public long count() {
        // Đếm tổng số driver trong hệ thống
        return driverRepository.count();
    }

    @Override
    public long countByStatus(DriverStatus driverStatus) {
        // Đếm số driver theo trạng thái (ACTIVE/INACTIVE/...)
        return driverRepository.countByStatus(driverStatus);
    }

    @Override
    public Optional<Driver> findByUserIdWithUser(Long userId) {
        // Tìm driver kèm User theo userId, trả về Optional
        return driverRepository.findByUserIdWithUser(userId);
    }

    @Override
    public void save(Driver driver) {
        // Hàm tiện ích: lưu driver (create/update)
        driverRepository.save(driver);
    }

}
