package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.UpdateAdminProfileRequest;
import com.swp391.gr3.ev_management.dto.request.UpdatePasswordRequest;
import com.swp391.gr3.ev_management.entity.Admin;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.AdminRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateAdminProfileRequest request) {
        Admin admin = adminRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Admin not found with userId " + userId));

        User user = admin.getUser();

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new ErrorException("Số điện thoại không được để trống");
        }

        String newPhone = request.getPhoneNumber().trim();

        // Kiểm tra định dạng (theo regex của bạn trong entity)
        if (!newPhone.matches("^(\\+84|0)\\d{9,10}$")) {
            throw new ErrorException("Số điện thoại không hợp lệ");
        }

        // Kiểm tra trùng với người khác
        boolean exists = userRepository.existsByPhoneNumber(newPhone);
        if (exists && !newPhone.equals(user.getPhoneNumber())) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        user.setPhoneNumber(newPhone);
        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        Admin admin = adminRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Admin not found with userId " + userId));

        User user = admin.getUser();

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1) Kiểm tra mật khẩu cũ
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu cũ không đúng");
        }

        // 2) Kiểm tra độ dài
        if (newPassword == null || newPassword.length() < 6) {
            throw new ErrorException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // 3) Không được trùng mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // 4) Xác nhận mật khẩu
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new ErrorException("Xác nhận mật khẩu mới không khớp");
        }

        // 5) Cập nhật
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
