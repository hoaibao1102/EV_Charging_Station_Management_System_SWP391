package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.dto.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.dto.response.StaffResponse;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.StaffMapper;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service // Đánh dấu class này là 1 Spring Service (chứa logic nghiệp vụ cho Staff)
@RequiredArgsConstructor // Lombok tự tạo constructor cho các field final để DI
public class StaffServiceImpl implements StaffService {

    private final StaffsRepository staffsRepository; // Repository thao tác với bảng Staffs
    private final StaffMapper staffMapper;            // Mapper chuyển Staffs -> StaffResponse

    // 👇 thêm vào
    private final UserRepository userRepository;     // Repository thao tác với bảng User
    private final PasswordEncoder passwordEncoder;   // Dùng để mã hoá và kiểm tra mật khẩu

    @Override
    @Transactional // Có thao tác ghi DB (update status) nên cần transaction
    public StaffResponse updateStatus(Long userId, StaffStatus status) {
        // 1) Tìm Staff theo userId, join fetch luôn User (theo method tuỳ chỉnh)
        Staffs staffs = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Staff not found with userId " + userId));

        // 2) Cập nhật trạng thái Staff (ACTIVE, INACTIVE, SUSPENDED, ...)
        staffs.setStatus(status);

        // 3) Lưu lại thông tin Staff sau khi chỉnh sửa
        staffsRepository.save(staffs);

        // 4) Map sang DTO trả về cho client
        return staffMapper.toStaffResponse(staffs);
    }

    /** ✅ Cập nhật hồ sơ Staff (fullName/email/phoneNumber) */
    @Override
    @Transactional // Có update thông tin User → cần transaction
    public StaffResponse updateProfile(Long userId, UpdateStaffProfileRequest request) {
        // 1) Lấy Staff theo userId, kèm User liên quan
        Staffs staff = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Staff not found with userId " + userId));

        // 2) Lấy entity User gắn với Staff này (chứa thông tin tài khoản cá nhân)
        User user = staff.getUser();

        // 3) Cập nhật từng field nếu request có giá trị (không null/blank)

        // fullName
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setName(request.getFullName().trim());
        }

        // dateOfBirth
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        // gender (ở đây chỉ set trực tiếp, nếu muốn có thể chuẩn hoá thêm: "M"/"F"/"OTHER", ...)
        if (request.getGender() != null && !request.getGender().isBlank()) {
            user.setGender(request.getGender());
        }

        // address
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            user.setAddress(request.getAddress().trim());
        }

        // 4) Lưu lại User sau khi cập nhật các trường
        userRepository.save(user);

        // 5) Map lại thông tin Staff (đã chứa User với dữ liệu mới) ra response
        return staffMapper.toStaffResponse(staff);
    }


    /** ✅ Đổi mật khẩu Staff */
    @Override
    @Transactional // Đổi mật khẩu cần ghi DB nên phải có transaction
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        // 1) Tìm Staff theo userId, join fetch luôn User
        Staffs staff = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Staff not found with userId " + userId));

        // 2) Lấy User từ Staff (User chứa passwordHash)
        User user = staff.getUser();

        // 3) Kiểm tra mật khẩu cũ có đúng không
        //    - passwordEncoder.matches(raw, encoded) → true nếu khớp
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu cũ không đúng");
        }

        // 4) Kiểm tra độ dài mật khẩu mới (>= 6 ký tự)
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ErrorException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // ✅ 5) Kiểm tra confirm password:
        //    - Không null
        //    - Phải trùng với newPassword
        if (request.getConfirmPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ErrorException("Xác nhận mật khẩu mới không khớp");
        }

        // 6) Không cho phép mật khẩu mới trùng với mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // 7) Mã hoá mật khẩu mới và set lại cho User
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // 8) Lưu User vào DB
        userRepository.save(user);
    }

    @Override
    public List<StaffResponse> getAll() {
        // 1) Lấy toàn bộ Staff từ DB
        // 2) Map sang StaffResponse để trả cho client
        return staffsRepository.findAll()
                .stream()
                .map(staffMapper::toStaffResponse)
                .toList();
    }

    @Override
    public Optional<Long> findIdByUserId(Long userId) {
        // Lấy staffId (Long) từ userId (dùng Optional để tránh null)
        return staffsRepository.findIdByUserId(userId);
    }

    @Override
    public Optional<Staffs> findByUser_UserId(Long userId) {
        // Tìm Staff entity theo userId (quan hệ Staff.user.userId)
        return staffsRepository.findByUser_UserId(userId);
    }

    @Override
    public long count() {
        // Đếm tổng số bản ghi Staff trong DB
        return staffsRepository.count();
    }

    @Override
    public long countByStatus(StaffStatus staffStatus) {
        // Đếm số Staff theo trạng thái (ACTIVE, INACTIVE, ...)
        return staffsRepository.countByStatus(staffStatus);
    }

    @Override
    public void save(Staffs staff) {
        // Lưu trực tiếp entity Staff (dùng cho các chỗ nghiệp vụ khác)
        staffsRepository.save(staff);
    }
}
