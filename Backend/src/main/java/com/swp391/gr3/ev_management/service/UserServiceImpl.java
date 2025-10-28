package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.LoginRequest;
import com.swp391.gr3.ev_management.DTO.request.RegisterRequest;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.DTO.request.DriverRequest;
import com.swp391.gr3.ev_management.enums.DriverStatus;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.events.UserRegisteredEvent;
import com.swp391.gr3.ev_management.repository.*;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DriverService driverService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final StationStaffRepository staffRepo;
    private final ChargingStationRepository stationRepo;
    private final ApplicationEventPublisher publisher;
    private final StaffsRepository  staffsRepo;
    private final StationStaffRepository stationStaffRepository;
    private final ChargingStationRepository chargingStationRepository;

    @Override
    public User findUsersByPhone(String phoneNumber) {
        return userRepository.findUsersByPhoneNumber(phoneNumber);
    }

    public User findUsersById(Long userId) {
        return userRepository.findUserByUserId(userId);
    }

    @Override
    public User getUser(String phoneNumber, String password) {
        return userRepository.findUsersByPhoneNumberAndPasswordHash(phoneNumber, password);
    }

    @Transactional
    @Override
    public User register(RegisterRequest r) {
        if (r.getPasswordHash() == null || r.getPasswordHash().isBlank())
            throw new IllegalArgumentException("Password is required");
        if (userRepository.existsByPhoneNumber(r.getPhoneNumber()))
            throw new IllegalStateException("Phone number already in use");
        if (r.getEmail() != null && userRepository.existsByEmail(r.getEmail()))
            throw new IllegalStateException("Email already in use");

        Role role = roleRepository.findByRoleId(3L); // default USER
        if (role == null) throw new IllegalStateException("Role not found");

        User u = new User();
        u.setEmail(r.getEmail());
        u.setPhoneNumber(r.getPhoneNumber());
        u.setPasswordHash(passwordEncoder.encode(r.getPasswordHash()));
        u.setName(r.getName());
        u.setDateOfBirth(r.getDateOfBirth());
        u.setGender(r.getGender());
        u.setAddress(r.getAddress());
        u.setRole(role);

        // 1) SAVE trước để có ID
        u = userRepository.save(u);

        //If là role driver create profile driver-active
        if(u.getRole().getRoleId() == 3L) {
            log.info("Auto-driver profile for user{} ", u.getUserId());

            DriverRequest driverReq = new DriverRequest();
            driverReq.setDriverStatus(DriverStatus.ACTIVE);

            driverService.createDriverProfile(u.getUserId(), driverReq);
        }

        // 2) PUBLISH event (listener sẽ tự tạo Notification)
        publisher.publishEvent(new UserRegisteredEvent(u.getUserId(), u.getEmail(), u.getName()));

        return u;
    }

    public User login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhoneNumber(),
                        loginRequest.getPassword()
                )
        );

        // Get the username (phone number) from the principal
        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // Fetch your Users entity from the database
        return userRepository.findUsersByPhoneNumber(springUser.getUsername());
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        return userRepository.existsByPhoneNumber(phoneNumber.trim());
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("No Bearer token found");
        }

        String token = authHeader.substring(7).trim();
        // Lấy thời điểm expire từ token
        Instant expiry;
        try {
            expiry = tokenService.getExpirationFromJwt(token); // trả về Instant
        } catch (Exception ex) {
            // Nếu token không hợp lệ, vẫn xóa cookie/client side nhưng server-side không cần lưu
            return ResponseEntity.badRequest().body("Invalid token");
        }

        long ttlSeconds = expiry.getEpochSecond() - Instant.now().getEpochSecond();
        if (ttlSeconds <= 0) {
            // token đã hết hạn => không cần blacklist
            return ResponseEntity.ok("Token already expired");
        }

        // nếu bạn đang gửi token qua cookie, trả cookie xoá ở đây (tùy app)
        return ResponseEntity.ok("Logged out successfully");
    }

    @Override
    public User authenticate(String phoneNumber, String rawPassword) {
        // 1️⃣ Kiểm tra user tồn tại
        User user = userRepository.findUsersByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new IllegalArgumentException("Số điện thoại không tồn tại");
        }

        // 2️⃣ Kiểm tra trạng thái hoạt động (tuỳ role)
        boolean isDriverActive = user.getDriver() != null
                && user.getDriver().getStatus() == DriverStatus.ACTIVE;

        boolean isStaffActive = user.getStaffs() != null
                && user.getStaffs().getStatus() == StaffStatus.ACTIVE;

        boolean isAdmin = user.getRole() != null
                && user.getRole().getRoleName().equals("ADMIN");

        if (!isDriverActive && !isStaffActive && !isAdmin) {
            throw new IllegalArgumentException("Tài khoản của bạn đang bị khóa hoặc không hoạt động");
        }

        // 3️⃣ Kiểm tra mật khẩu
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không chính xác");
        }

        // ✅ Thành công
        return user;
    }


    @Override
    public void addUser(User user) {
        userRepository.save(user);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAllWithJoins();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findUserByUserId(id);
    }

    // ===== ADMIN: đăng ký user và biến thành StationStaff của một trạm =====
    @Override
    @Transactional
    public User registerAsStaff(RegisterRequest req, Long stationId) {
        if (userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("Email đã tồn tại");
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) throw new IllegalArgumentException("Số điện thoại đã tồn tại");

        Role staffRole = roleRepository.findByRoleName("STAFF");
        if (staffRole == null) throw new IllegalStateException("Role STAFF chưa được seed");

        // 1) Tạo user
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

        // 2) Tạo Staffs
        Staffs staff = Staffs.builder()
                .user(user)
                .status(StaffStatus.ACTIVE)
                .roleAtStation("STAFF")
                .build();
        staff = staffsRepo.save(staff);

        // 3) Gán vào Station_Staff (nếu có stationId)
        if (stationId != null) {
            var station = stationRepo.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station không tồn tại: " + stationId));

            // (khuyến nghị) chặn 1 staff có hơn 1 assignment active
            boolean hasActive = staffRepo.existsByStaff_StaffIdAndUnassignedAtIsNull(staff.getStaffId());
            if (hasActive) {
                throw new IllegalStateException("Staff đã được gán station khác và chưa unassign.");
            }

            var link = StationStaff.builder()
                    .staff(staff)
                    .station(station)
                    .assignedAt(LocalDateTime.now())
                    .build();

            link = staffRepo.save(link);

            // Nếu muốn cập nhật quan hệ 2 chiều trong bộ nhớ (không bắt buộc):
            // staff.getStationStaffs().add(link);
        }


        return user;
    }

    @Transactional
    public Map<String, Object> registerStaffAndAssignStation(RegisterRequest req, Long stationId) {
        // 1) Gọi lại logic cũ để tạo User + Staff (giữ nguyên)
        User user = registerAsStaff(req, stationId);

        // 2) Lấy Staff theo user
        Staffs staff = staffsRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy staff cho user " + user.getUserId()));

        Long stationStaffId = null;

        // 3) Nếu có stationId thì gán vào Station_Staff
        if (stationId != null) {
            // 3.1) Kiểm tra station tồn tại
            ChargingStation station = chargingStationRepository.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station không tồn tại với id=" + stationId));

            // 3.2) Đóng assignment cũ (nếu có)
            stationStaffRepository.findActiveByStaffId(staff.getStaffId())
                    .ifPresent(active -> {
                        active.setUnassignedAt(LocalDateTime.now());
                        stationStaffRepository.save(active);
                    });

            // 3.3) Tạo assignment mới
            StationStaff newAssign = StationStaff.builder()
                    .staff(staff)
                    .station(station)
                    .assignedAt(LocalDateTime.now())
                    .unassignedAt(null)
                    .build();
            StationStaff saved = stationStaffRepository.save(newAssign);
            stationStaffId = saved.getStationStaffId();
        }

        // 4) Thông báo cho staff mới (giữ nguyên)
        publisher.publishEvent(new UserRegisteredEvent(user.getUserId(), user.getEmail(), user.getName()));

        // 5) Trả về kết quả (giữ nguyên keys cũ)
        return Map.of(
                "message", "Đăng ký staff thành công",
                "userId", user.getUserId(),
                "staffId", staff.getStaffId(),
                "stationId", stationId,
                "stationStaffId", stationStaffId
        );
    }

}
