package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.LoginRequest;
import com.swp391.gr3.ev_management.dto.request.RegisterRequest;
import com.swp391.gr3.ev_management.dto.response.GetUsersResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.dto.request.DriverRequest;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.events.UserRegisteredEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.UserResponseMapper;
import com.swp391.gr3.ev_management.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional; // <-- dùng Spring
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j // Tự động sinh logger (log.info, log.error, ...)
@Service // Đánh dấu đây là Spring Service xử lý nghiệp vụ liên quan đến User
@RequiredArgsConstructor // Lombok tạo constructor cho các field final
public class UserServiceImpl implements UserService{

    // ====== Dependencies inject qua constructor ======
    private final UserRepository userRepository;                 // Làm việc với bảng Users
    private final RoleService roleService;                       // Lấy Role (USER/STAFF/ADMIN,...)
    private final DriverService driverService;                   // Nghiệp vụ cho Driver (tự tạo profile driver,...)
    private final AuthenticationManager authenticationManager;   // Spring Security auth manager
    private final PasswordEncoder passwordEncoder;               // Mã hoá & kiểm tra mật khẩu
    private final TokenService tokenService;                     // Xử lý JWT (ở chỗ logout)
    private final ApplicationEventPublisher publisher;           // Publish event (UserRegisteredEvent)
    private final StaffService  staffService;                    // Nghiệp vụ Staff
    private final StationStaffService stationStaffService;       // Quản lý mapping Staff <-> Station
    private final ChargingStationService chargingStationService; // Nghiệp vụ trạm sạc (dùng khi assign staff)
    private final ChargingSessionService chargingSessionService; // Dùng để đếm số sessions theo user
    private final UserResponseMapper userResponseMapper;         // Map User -> GetUsersResponse

    /**
     * Lấy User theo phoneNumber + passwordHash (cũ, không dùng Spring Security).
     * (Hàm này có vẻ để tương thích với code cũ; hiện tại login() dùng AuthenticationManager rồi.)
     */
    @Override
    public User getUser(String phoneNumber, String password) {
        return userRepository.findUsersByPhoneNumberAndPasswordHash(phoneNumber, password);
    }

    /**
     * Đăng ký tài khoản user thường (mặc định roleId=3L).
     * - Validate password không rỗng
     * - Kiểm tra trùng số điện thoại, email
     * - Gán role mặc định
     * - Tạo User, lưu DB
     * - Nếu role là DRIVER thì auto tạo Driver profile ACTIVE
     * - Publish event UserRegisteredEvent (để send email / notification)
     */
    @Transactional
    @Override
    public User register(RegisterRequest r) {
        // 1) Validate password
        if (r.getPasswordHash() == null || r.getPasswordHash().isBlank())
            throw new ErrorException("Password is required");
        // 2) Kiểm tra trùng phone
        if (userRepository.existsByPhoneNumber(r.getPhoneNumber()))
            throw new ErrorException("Phone number already in use");
        // 3) Kiểm tra trùng email (nếu có email)
        if (r.getEmail() != null && userRepository.existsByEmail(r.getEmail()))
            throw new ErrorException("Email already in use");

        // 4) Lấy role mặc định (roleId = 3L -> USER/DRIVER tuỳ seed)
        Role role = roleService.findByRoleId(3L); // default USER
        if (role == null) throw new IllegalStateException("Role not found");

        // 5) Tạo entity User từ request
        User u = new User();
        u.setEmail(r.getEmail());
        u.setPhoneNumber(r.getPhoneNumber());
        u.setPasswordHash(passwordEncoder.encode(r.getPasswordHash())); // mã hoá password
        u.setName(r.getName());
        u.setDateOfBirth(r.getDateOfBirth());
        u.setGender(r.getGender());
        u.setAddress(r.getAddress());
        u.setRole(role);

        // 6) Lưu User trước để có userId (PK)
        u = userRepository.save(u);

        // 7) Nếu role là driver (id = 3L) thì auto tạo driver profile ACTIVE
        if(u.getRole().getRoleId() == 3L) {
            log.info("Auto-driver profile for user{} ", u.getUserId());

            DriverRequest driverReq = new DriverRequest();
            driverReq.setDriverStatus(DriverStatus.ACTIVE); // mặc định ACTIVE

            driverService.createDriverProfile(u.getUserId(), driverReq);
        }

        // 8) Publish event để các listener xử lý (gửi email welcome,...)
        publisher.publishEvent(new UserRegisteredEvent(u.getUserId(), u.getEmail(), u.getName()));

        return u;
    }

    /**
     * Đăng nhập sử dụng Spring Security AuthenticationManager.
     * - Xác thực bằng phoneNumber + password.
     * - Sau khi auth thành công, lấy principal (username = phoneNumber).
     * - Load entity Users tương ứng từ DB và trả về.
     */
    public User login(LoginRequest loginRequest) {
        // 1) Dùng AuthenticationManager để xác thực
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhoneNumber(),
                        loginRequest.getPassword()
                )
        );

        // 2) Lấy principal sau khi đã authenticate thành công
        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // 3) Dùng phoneNumber (username) để load entity User từ DB
        return userRepository.findUsersByPhoneNumber(springUser.getUsername());
    }

    /**
     * Kiểm tra số điện thoại đã tồn tại trong hệ thống chưa.
     */
    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        return userRepository.existsByPhoneNumber(phoneNumber.trim());
    }

    /**
     * Kiểm tra email đã tồn tại trong hệ thống chưa.
     */
    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    /**
     * Đăng xuất (logout) dựa trên JWT trong header Authorization.
     * - Ở đây chỉ kiểm tra token & trả về message, chưa lưu blacklist.
     * - Có thể mở rộng: đưa token vào blacklist cho đến khi hết hạn.
     */
    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        // 1) Kiểm tra có header Authorization và bắt đầu bằng "Bearer " hay không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("No Bearer token found");
        }

        // 2) Lấy token (cắt "Bearer ")
        String token = authHeader.substring(7).trim();
        // 3) Lấy thời điểm hết hạn từ token
        Instant expiry;
        try {
            expiry = tokenService.getExpirationFromJwt(token); // trả về Instant exp
        } catch (Exception ex) {
            // Token không hợp lệ -> báo lỗi
            return ResponseEntity.badRequest().body("Invalid token");
        }

        // 4) Tính TTL còn lại
        long ttlSeconds = expiry.getEpochSecond() - Instant.now().getEpochSecond();
        if (ttlSeconds <= 0) {
            // Token đã hết hạn -> xem như logout ok, không cần blacklist
            return ResponseEntity.ok("Token already expired");
        }

        // 5) Nếu cần có thể lưu token vào blacklist với TTL = ttlSeconds
        // (Ở code hiện tại chỉ trả về message)
        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * Authenticate tự viết (không dùng AuthenticationManager) cho mobile/api đơn giản.
     * - Kiểm tra user tồn tại theo phoneNumber.
     * - Kiểm tra trạng thái tài khoản (Driver/Staff/Admin).
     * - Kiểm tra mật khẩu có khớp không.
     * - Nếu mọi thứ ok -> trả về User.
     */
    @Override
    public User authenticate(String phoneNumber, String rawPassword) {
        // 1️⃣ Kiểm tra user tồn tại
        User user = userRepository.findUsersByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new ErrorException("Số điện thoại không tồn tại");
        }

        // 2️⃣ Kiểm tra trạng thái hoạt động (tuỳ role)
        boolean isDriverActive = user.getDriver() != null
                && user.getDriver().getStatus() == DriverStatus.ACTIVE;

        boolean isStaffActive = user.getStaffs() != null
                && user.getStaffs().getStatus() == StaffStatus.ACTIVE;

        boolean isAdmin = user.getRole() != null
                && user.getRole().getRoleName().equals("ADMIN");

        // Nếu không phải admin, không phải driver active, không phải staff active -> khoá
        if (!isDriverActive && !isStaffActive && !isAdmin) {
            throw new ErrorException("Tài khoản của bạn đang bị khóa hoặc không hoạt động");
        }

        // 3️⃣ Kiểm tra mật khẩu (so sánh hash)
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu không chính xác");
        }

        // ✅ Thành công -> trả về user
        return user;
    }

    /**
     * Thêm user mới (dùng nội bộ).
     */
    @Override
    public void addUser(User user) {
        userRepository.save(user);
    }

    /**
     * Lấy tất cả user (dùng query findAllWithJoins để fetch đầy đủ thông tin liên quan).
     */
    @Override
    public List<User> findAll() {
        return userRepository.findAllWithJoins();
    }

    /**
     * Tìm user theo userId.
     */
    @Override
    public User findById(Long id) {
        return userRepository.findUserByUserId(id);
    }

    // ===== ADMIN: đăng ký user và biến thành Staff (chưa assign station) =====
    /**
     * Đăng ký một Staff mới (ADMIN dùng).
     * - Kiểm tra trùng email/phone.
     * - Gán role STAFF.
     * - Tạo User.
     * - Tạo Staffs gắn với User (status ACTIVE).
     */
    @Override
    @Transactional
    public User registerAsStaff(RegisterRequest req, Long stationId) {
        // 1) Kiểm tra trùng email/phone
        if (userRepository.existsByEmail(req.getEmail())) throw new ErrorException("Email đã tồn tại");
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) throw new ErrorException("Số điện thoại đã tồn tại");

        // 2) Lấy role STAFF
        Role staffRole = roleService.findByRoleName("STAFF");
        if (staffRole == null) throw new ErrorException("Role STAFF chưa được seed");

        // 3) Tạo user mới với role STAFF
        User user = User.builder()
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(req.getPasswordHash()))
                .name(req.getName())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .address(req.getAddress())
                .role(staffRole)
                .build();
        user = userRepository.save(user);

        // 4) Tạo Staffs entity gắn với user
        Staffs staff = Staffs.builder()
                .user(user)
                .status(StaffStatus.ACTIVE)
                .roleAtStation("STAFF")
                .build();
        staffService.save(staff); // lưu Staffs

        return user;
    }

    /**
     * Đăng ký Staff và gán luôn vào một Station cụ thể.
     * - Gọi lại registerAsStaff để tạo User + Staff.
     * - Gán Staff vào StationStaff (đóng assignment cũ nếu có).
     * - Publish UserRegisteredEvent.
     * - Trả về map chứa thông tin kết quả.
     */
    @Transactional
    public Map<String, Object> registerStaffAndAssignStation(RegisterRequest req, Long stationId) {
        // 1) Gọi lại logic cũ để tạo User + Staff
        User user = registerAsStaff(req, stationId);

        // 2) Lấy Staff theo user
        Staffs staff = staffService.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ErrorException("Không tìm thấy staff cho user " + user.getUserId()));

        Long stationStaffId = null;

        // 3) Nếu có stationId thì gán vào bảng StationStaff
        if (stationId != null) {
            // 3.1) Kiểm tra station tồn tại
            ChargingStation station = chargingStationService.findById(stationId)
                    .orElseThrow(() -> new ErrorException("Station không tồn tại với id=" + stationId));

            // 3.2) Đóng assignment cũ (nếu đang active ở trạm khác)
            stationStaffService.findActiveByStaffId(staff.getStaffId())
                    .ifPresent(active -> {
                        active.setUnassignedAt(LocalDateTime.now());
                        stationStaffService.save(active);
                    });

            // 3.3) Tạo assignment mới (Staff gán vào trạm mới)
            StationStaff newAssign = StationStaff.builder()
                    .staff(staff)
                    .station(station)
                    .assignedAt(LocalDateTime.now())
                    .unassignedAt(null)
                    .build();
            StationStaff saved = stationStaffService.saveStationStaff(newAssign);
            stationStaffId = saved.getStationStaffId();
        }

        // 4) Gửi event cho staff mới (email, thông báo,...)
        publisher.publishEvent(new UserRegisteredEvent(user.getUserId(), user.getEmail(), user.getName()));

        // 5) Trả về thông tin tổng hợp
        return Map.of(
                "message", "Đăng ký staff thành công",
                "userId", user.getUserId(),
                "staffId", staff.getStaffId(),
                "stationId", stationId,
                "stationStaffId", stationStaffId
        );
    }

    /**
     * Lấy danh sách tất cả Users kèm số lượng phiên sạc (sessionCount) của từng user.
     * - Dùng cho trang quản trị.
     */
    @Override
    public List<GetUsersResponse> getAllUsersWithSessions() {
        return userRepository.findAllWithJoins()
                .stream()
                .map(user -> {
                    // Đếm số phiên sạc mà user đã tham gia (thông qua ChargingSessionService)
                    long sessionCount = chargingSessionService.countSessionsByUserId(user.getUserId());
                    // Map sang DTO GetUsersResponse (gồm info user + sessionCount)
                    return userResponseMapper.toGetUsersResponse(user, sessionCount);
                })
                .toList();
    }

    /**
     * Tìm User theo email.
     */
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Tìm User theo userId.
     */
    @Override
    public User findUserByUserId(Long userId) {
        return userRepository.findUserByUserId(userId);
    }

    /**
     * Đếm tổng số user trong hệ thống.
     * - Dùng cho dashboard thống kê.
     */
    @Override
    public long count() {
        return userRepository.count();
    }

}
