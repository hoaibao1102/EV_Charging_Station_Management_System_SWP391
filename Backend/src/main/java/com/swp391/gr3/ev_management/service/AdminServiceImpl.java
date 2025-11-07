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

@Service // Đánh dấu đây là một Spring Service (bean xử lý logic nghiệp vụ)
@RequiredArgsConstructor // Tự động tạo constructor chứa các field final
@Slf4j // Tích hợp logger (log.info, log.error, ...)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository; // Repository để truy xuất bảng Admin
    private final UserRepository userRepository;   // Repository để truy xuất bảng User
    private final PasswordEncoder passwordEncoder; // Dùng để mã hóa & kiểm tra mật khẩu

    @Override
    @Transactional // Đảm bảo toàn bộ thao tác được thực hiện trong 1 transaction
    public void updateProfile(Long userId, UpdateAdminProfileRequest request) {
        // Lấy ra admin kèm theo thông tin user dựa vào userId
        Admin admin = adminRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Admin not found with userId " + userId));

        User user = admin.getUser(); // Lấy user liên kết với admin

        // Kiểm tra nếu số điện thoại rỗng hoặc null
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new ErrorException("Số điện thoại không được để trống");
        }

        String newPhone = request.getPhoneNumber().trim(); // Xóa khoảng trắng thừa

        // Kiểm tra định dạng số điện thoại theo regex (phải bắt đầu bằng +84 hoặc 0 và có 9-10 số)
        if (!newPhone.matches("^(\\+84|0)\\d{9,10}$")) {
            throw new ErrorException("Số điện thoại không hợp lệ");
        }

        // Kiểm tra xem số điện thoại này đã tồn tại trong hệ thống chưa
        boolean exists = userRepository.existsByPhoneNumber(newPhone);
        // Nếu đã tồn tại và không phải của user hiện tại thì báo lỗi trùng
        if (exists && !newPhone.equals(user.getPhoneNumber())) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        // Cập nhật số điện thoại mới
        user.setPhoneNumber(newPhone);
        // Lưu lại vào cơ sở dữ liệu
        userRepository.save(user);
    }

    @Transactional // Đảm bảo transaction cho quá trình cập nhật mật khẩu
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        // Lấy ra admin và user tương ứng
        Admin admin = adminRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ErrorException("Admin not found with userId " + userId));

        User user = admin.getUser(); // Lấy đối tượng user

        // Lấy thông tin mật khẩu cũ, mới và xác nhận mật khẩu mới từ request
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1) Kiểm tra mật khẩu cũ có đúng không
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu cũ không đúng");
        }

        // 2) Kiểm tra độ dài mật khẩu mới (ít nhất 6 ký tự)
        if (newPassword == null || newPassword.length() < 6) {
            throw new ErrorException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // 3) Không được trùng với mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ErrorException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // 4) Kiểm tra mật khẩu xác nhận có khớp không
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new ErrorException("Xác nhận mật khẩu mới không khớp");
        }

        // 5) Mã hóa mật khẩu mới và cập nhật vào DB
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
