package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.exception.NotFoundException;
import com.swp391.gr3.ev_management.repository.OtpRepository;
import com.swp391.gr3.ev_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final OtpService otpService;             // bạn đã có OtpServiceImpl
    private final PasswordEncoder passwordEncoder;   // nhớ khai báo bean PasswordEncoder

    @Override
    @Transactional
    public void sendResetOtp(String email) {
        // 1) Kiểm tra user tồn tại (tránh lộ thông tin: có thể luôn trả "đã gửi")
        User user = userRepository.findByEmail(email);
        if (user == null) {
            // tuỳ policy: hoặc im lặng coi như đã gửi, hoặc báo không tồn tại
            throw new NotFoundException("Email không tồn tại trong hệ thống");
        }
        // 2) Sinh và gửi OTP qua email (đã có template "email-otp")
        otpService.generateOtp(email);
        log.info("Password reset OTP generated for {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyResetOtp(String email, String otp) {
        // gọi thẳng service verify (có mark verified=true nếu hợp lệ)
        return otpService.verifyOtp(email, otp);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // 1) Lấy OTP mới nhất
        OtpVerification latest = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new NotFoundException("OTP không tồn tại."));

        // 2) Kiểm tra còn hạn & khớp mã
        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP đã hết hạn.");
        }
        if (!latest.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("OTP không đúng.");
        }
        if (latest.isVerified() == false) {
            // nếu muốn yêu cầu bước verify riêng, có thể bắt buộc latest.isVerified()==true
            // hoặc tự mark verified tại đây:
            latest.setVerified(true);
        }

        // 3) Đổi mật khẩu
        User user = userRepository.findByEmail(email);
        if (user == null) throw new NotFoundException("Email không tồn tại.");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4) (tuỳ chọn) vô hiệu các OTP cũ khác hoặc cập nhật consumedAt
        latest.setVerified(true);
        otpRepository.save(latest);
        log.info("Password reset successfully for {}", email);
    }
}
