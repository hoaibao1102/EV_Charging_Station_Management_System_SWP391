package com.swp391.gr3.ev_management.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service // Đánh dấu lớp này là Spring Service (dùng cho gửi email)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
@Slf4j // Cho phép sử dụng logger log.info(), log.error(), ...
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;        // Dùng để gửi email qua SMTP
    private final TemplateEngine templateEngine;    // Thymeleaf template engine để render HTML email

    // Đọc địa chỉ email mặc định từ file cấu hình (application.yml hoặc properties)
    @Value("${app.notifications.email.from:no-reply@evms.local}")
    private String from;

    // ========= 1) Gửi email HTML đã render sẵn (legacy-compatible) =========
    // (hiện chưa có trong đoạn code này, nhưng phần này dùng cho các email tĩnh hoặc đã có nội dung HTML sẵn)

    // ========= 2) Gửi notification bằng Thymeleaf template =========
    @Async // Gửi email bất đồng bộ (không chặn luồng chính, chạy nền)
    @Override
    public void sendNotificationEmailTpl(String to,
                                         String subject,
                                         String displayName,
                                         Object title, Object body,
                                         Object type, Object status, Object createdAt) {
        try {
            // Tạo context (biến truyền vào template Thymeleaf)
            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("title", title);
            ctx.setVariable("body", body);
            ctx.setVariable("type", type);
            ctx.setVariable("status", status);
            ctx.setVariable("createdAt", createdAt);

            // Render template email-notification.html thành chuỗi HTML
            String html = templateEngine.process("email-notification", ctx);

            // Tạo đối tượng email MIME (hỗ trợ HTML, file đính kèm, v.v.)
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            // Thiết lập thông tin cơ bản
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = gửi HTML content

            // Gửi email
            mailSender.send(msg);
        } catch (Exception e) {
            // Ghi log lỗi, không ném lỗi ra ngoài để không làm gián đoạn luồng chính
            log.error("sendNotificationEmailTpl failed", e);
        }
    }

    /**
     * Gửi email thông báo khi booking bị hủy
     * - Sử dụng template "booking-cancelled.html"
     * - Truyền các biến bookingId, stationName, timeRange vào template
     */
    @Override
    public void sendBookingCancelledTpl(String to, String subject, String displayName,
                                        Long bookingId, String stationName, String timeRange) {
        // Tạo email MIME
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            // Dùng MimeMessageHelper để giúp set thông tin MIME (to, subject, from, content)
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("no-reply@evms.local"); // Có thể thay đổi domain nếu cần

            // Tạo context Thymeleaf cho template
            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("bookingId", bookingId);
            ctx.setVariable("stationName", stationName);
            ctx.setVariable("timeRange", timeRange);

            // Render HTML từ template "booking-cancelled.html"
            String html = templateEngine.process("booking-cancelled", ctx);
            helper.setText(html, true); // true = gửi HTML

            // (Tùy chọn) Đính kèm logo inline nếu có
            // helper.addInline("logo", new ClassPathResource("static/email/logo.png"), "image/png");

            // Gửi email
            mailSender.send(mime);
        } catch (Exception e) {
            // Nếu lỗi -> ném RuntimeException để log dễ debug
            throw new RuntimeException("Failed to send booking-cancelled email", e);
        }
    }

    /**
     * Gửi email xác nhận đặt chỗ (booking confirmed)
     * - Dùng template "booking-confirmed.html"
     * - Có thể đính kèm QR code (ảnh PNG) inline trong email
     */
    @Override
    public void sendBookingConfirmedTpl(String to,
                                        String subject,
                                        String displayName,
                                        Long bookingId,
                                        String station,
                                        String timeRange,
                                        String slotName,
                                        String connectorType,
                                        byte[] qrBytes) {
        try {
            // Tạo email MIME
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            // Tạo context Thymeleaf (các biến được binding vào template)
            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("bookingId", bookingId);
            ctx.setVariable("station", station);
            ctx.setVariable("timeRange", timeRange);
            ctx.setVariable("slotName", slotName);
            ctx.setVariable("connectorType", connectorType);
            ctx.setVariable("cid", "qr"); // Dùng trong template để hiển thị ảnh QR: th:src="'cid:' + ${cid}"

            // Render template "booking-confirmed.html" thành nội dung HTML
            String html = templateEngine.process("booking-confirmed", ctx);

            // Thiết lập thông tin email
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            // Nếu có dữ liệu QR code (ảnh PNG), đính kèm inline trong email
            if (qrBytes != null && qrBytes.length > 0) {
                helper.addInline("qr", new ByteArrayResource(qrBytes), "image/png");
            }

            // Gửi email
            mailSender.send(msg);
        } catch (Exception e) {
            // Nếu gửi thất bại, chỉ ghi log để không làm hỏng luồng nghiệp vụ chính
            System.err.println("[EmailService] Failed to send booking-confirmed email: " + e.getMessage());
        }
    }

    // ========= Helper =========
    // Hàm nhỏ để đảm bảo không bị NullPointerException khi hiển thị dữ liệu trong email
    private static String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    @Override
    public void sendPasswordEmailHtml(String to, String password) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("EV Station – Mật khẩu đăng nhập lần đầu");

            Context ctx = new Context();
            ctx.setVariable("password", password);
            String html = templateEngine.process("password-first-login", ctx);

            helper.setText(html, true);

            mailSender.send(msg);

        } catch (Exception e) {
            log.error("Failed to send HTML password email", e);
        }
    }
}
