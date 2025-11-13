package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import com.swp391.gr3.ev_management.repository.OtpRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service // Đánh dấu class này là 1 Spring Service xử lý logic OTP
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
public class OtpServiceImpl implements OtpService {

    // Repository thao tác với DB bảng otp_verification
    private final OtpRepository otpRepository;

    // Đối tượng gửi email
    private final JavaMailSender mailSender;

    // Engine để render HTML template (Thymeleaf)
    private final TemplateEngine templateEngine; // ✨ inject thymeleaf engine

    @Override
    public String generateOtp(String email) {
        // 1️⃣ Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();

        // 2️⃣ Lưu OTP vào DB (expires sau 5 phút)
        otpRepository.save(OtpVerification.builder()
                .email(email)
                .otpCode(otp)
                .createdAt(now)
                .expiresAt(now.plusMinutes(5)) // Set thời gian hết hạn
                .verified(false)               // Trạng thái ban đầu chưa verify
                .build());

        try {
            // 3️⃣ Render nội dung email từ template Thymeleaf
            Context context = new Context();  // Context chứa biến để truyền vào template
            context.setVariable("otp", otp);  // Gửi OTP vào HTML template

            // 4️⃣ Process template để tạo HTML email hoàn chỉnh
            String htmlBody = templateEngine.process("email-otp", context);

            // 5️⃣ Tạo email MIME (email dạng HTML)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);                                     // Email người nhận
            helper.setSubject("🔐 [EV Management] Mã xác thực OTP"); // Tiêu đề email
            helper.setText(htmlBody, true);                          // True = HTML email

            // 6️⃣ Gửi email
            mailSender.send(message);
            System.out.println("Sent OTP email to " + email);

        } catch (Exception e) {
            // 7️⃣ Bắt lỗi gửi email, không làm crash request
            e.printStackTrace();
            System.out.println("Failed to send HTML mail: " + e.getMessage());
        }

        // 8️⃣ Trả OTP (dùng nội bộ test hoặc nếu cần verify thủ công)
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        // 1️⃣ Tìm OTP mới nhất theo email
        Optional<OtpVerification> latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        if (latestOtp.isEmpty()) return false;

        OtpVerification otp = latestOtp.get();

        // 2️⃣ OTP đã được xác minh rồi → từ chối
        if (otp.isVerified()) return false;

        // 3️⃣ OTP hết hạn → từ chối
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        // 4️⃣ OTP không khớp → từ chối
        if (!otp.getOtpCode().equals(otpCode)) return false;

        // 5️⃣ OTP hợp lệ → đánh dấu đã verify
        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    @Override
    public Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email) {
        // Lấy OTP gần nhất theo email
        return otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
    }

    @Override
    public void save(OtpVerification latest) {
        // Lưu vào DB
        otpRepository.save(latest);
    }
}
