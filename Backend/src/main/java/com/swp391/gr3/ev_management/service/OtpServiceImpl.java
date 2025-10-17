package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import com.swp391.gr3.ev_management.repository.OtpRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements  OtpService{
    @Autowired
    private final OtpRepository otpRepository;
    @Autowired
    private final JavaMailSender mailSender;
    @Autowired
    private final TemplateEngine templateEngine; // ✨ inject thymeleaf engine

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();

        otpRepository.save(OtpVerification.builder()
                .email(email)
                .otpCode(otp)
                .createdAt(now)
                .expiresAt(now.plusMinutes(5))
                .verified(false)
                .build());

        try {
            // 🧩 Render HTML từ template Thymeleaf
            Context context = new Context();
            context.setVariable("otp", otp);

            String htmlBody = templateEngine.process("email-otp", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(email);
            helper.setSubject("🔐 [EV Management] Mã xác thực OTP");
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("Sent OTP email to " + email);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to send HTML mail: " + e.getMessage());
        }

        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        Optional<OtpVerification> latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        if (latestOtp.isEmpty()) return false;

        OtpVerification otp = latestOtp.get();
        if (otp.isVerified()) return false;
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!otp.getOtpCode().equals(otpCode)) return false;

        otp.setVerified(true);
        otpRepository.save(otp);
        return true;
    }
}
