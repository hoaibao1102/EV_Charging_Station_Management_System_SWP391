package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.DTO.request.UpdateStaffProfileRequest;
import com.swp391.gr3.ev_management.DTO.response.StaffResponse;
import com.swp391.gr3.ev_management.entity.Staffs;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.StaffStatus;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.mapper.StaffMapper;
import com.swp391.gr3.ev_management.repository.StaffsRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffsRepository staffsRepository;
    private final StaffMapper staffMapper;

    // 👇 thêm vào
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Staffs findByStaffId(Long staffId) {
        return staffsRepository.findByStaffId(staffId);
    }

    @Override
    @Transactional
    public StaffResponse updateStatus(Long userId, StaffStatus status) {
        Staffs staffs = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Staff not found with userId " + userId));
        staffs.setStatus(status);
        staffsRepository.save(staffs);
        return staffMapper.toStaffResponse(staffs);
    }

    /** ✅ Cập nhật hồ sơ Staff (fullName/email/phoneNumber) */
    @Override
    @Transactional
    public StaffResponse updateProfile(Long userId, UpdateStaffProfileRequest request) {
        Staffs staff = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Staff not found with userId " + userId));

        User user = staff.getUser();
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        userRepository.save(user);

        // map lại sang response
        return staffMapper.toStaffResponse(staff);
    }

    /** ✅ Đổi mật khẩu Staff */
    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        Staffs staff = staffsRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new NotFoundException("Staff not found with userId " + userId));

        User user = staff.getUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
