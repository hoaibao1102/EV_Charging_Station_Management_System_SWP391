package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service // Đánh dấu đây là một Spring Service (bean xử lý logic nghiệp vụ)
@RequiredArgsConstructor // Tự động sinh constructor với tất cả các trường final
@Slf4j // Cung cấp logger (dùng log.info, log.error,...)
public class AuthServiceImpl implements AuthService {

    // Repository để thao tác với bảng User
    private final UserService userService;
    // Service chuyên xử lý logic về OTP (gửi, sinh, xác minh, ...)
    private final OtpService otpService;
    // Dùng để mã hóa mật khẩu (BCrypt hoặc tương tự)
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional // Đảm bảo toàn bộ logic trong hàm nằm trong một transaction
    public void sendResetOtp(String email) {
        // 1) Kiểm tra xem email có tồn tại trong hệ thống hay không
        User user = userService.findByEmail(email);
        if (user == null) {
            // Nếu không tồn tại, ném lỗi (hoặc tùy chính sách, có thể trả về giả là "đã gửi")
            throw new ErrorException("Email không tồn tại trong hệ thống");
        }

        // 2) Nếu tồn tại -> sinh mã OTP và gửi qua email
        // OtpServiceImpl của bạn sẽ chịu trách nhiệm tạo mã, lưu DB và gửi email
        otpService.generateOtp(email);

        // Ghi log để theo dõi hành động
        log.info("Password reset OTP generated for {}", email);
    }

    @Override
    @Transactional // Bọc trong transaction để đảm bảo tính toàn vẹn dữ liệu
    public void resetPassword(String email, String otp, String newPassword) {
        // 1) Lấy OTP mới nhất của người dùng dựa vào email, sắp xếp theo thời gian tạo giảm dần
        OtpVerification latest = otpService.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ErrorException("OTP không tồn tại."));

        // 2) Kiểm tra xem OTP có còn hạn không (so sánh thời gian hết hạn với hiện tại)
        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP đã hết hạn.");
        }

        // 3) Kiểm tra OTP nhập vào có đúng với mã đã lưu trong DB không
        if (!latest.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("OTP không đúng.");
        }

        // 4) Nếu OTP chưa được đánh dấu là "verified", ta có thể set tại đây
        if (latest.isVerified() == false) {
            // Có thể yêu cầu xác minh OTP trước, nhưng ở đây ta đánh dấu là đã verified
            latest.setVerified(true);
        }

        // 5) Đổi mật khẩu cho user có email tương ứng
        User user = userService.findByEmail(email);
        if (user == null) throw new ErrorException("Email không tồn tại.");

        // Mã hóa mật khẩu mới bằng PasswordEncoder trước khi lưu
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Lưu thay đổi mật khẩu vào DB
        userService.addUser(user);

        // 6) (Tuỳ chọn) Cập nhật lại trạng thái OTP — ví dụ đánh dấu đã dùng
        latest.setVerified(true);
        otpService.save(latest);

        // 7) Ghi log thông báo reset thành công
        log.info("Password reset successfully for {}", email);
    }
}
