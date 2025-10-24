package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Notification;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf

    @Value("${app.notifications.email.from:no-reply@evms.local}")
    private String from;

    // ========= 1) Gửi email HTML đã render sẵn (legacy-compatible) =========
    @Async
    @Override
    public void sendNotificationEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            // TODO: logger.error("sendNotificationEmail failed", e);
        }
    }

    // ========= 2) Gửi notification bằng Thymeleaf template =========
    @Async
    @Override
    public void sendNotificationEmailTpl(String to,
                                         String subject,
                                         String displayName,
                                         Object title, Object body,
                                         Object type, Object status, Object createdAt) {
        try {
            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("title", title);
            ctx.setVariable("body", body);
            ctx.setVariable("type", type);
            ctx.setVariable("status", status);
            ctx.setVariable("createdAt", createdAt);

            String html = templateEngine.process("email-notification", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            // TODO: logger.error("sendNotificationEmailTpl failed", e);
        }
    }

    @Override
    public void sendBookingCancelledTpl(String to, String subject, String displayName,
                                        Long bookingId, String stationName, String timeRange) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("no-reply@evms.local"); // đổi domain của bạn

            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("bookingId", bookingId);
            ctx.setVariable("stationName", stationName);
            ctx.setVariable("timeRange", timeRange);

            String html = templateEngine.process("booking-cancelled", ctx);
            helper.setText(html, true);

            // (tuỳ chọn) gắn logo:
            // helper.addInline("logo", new ClassPathResource("static/email/logo.png"), "image/png");

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send booking-cancelled email", e);
        }
    }

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
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            Context ctx = new Context();
            ctx.setVariable("displayName", displayName);
            ctx.setVariable("bookingId", bookingId);
            ctx.setVariable("station", station);
            ctx.setVariable("timeRange", timeRange);
            ctx.setVariable("slotName", slotName);
            ctx.setVariable("connectorType", connectorType);
            ctx.setVariable("cid", "qr"); // used in template as th:src="'cid:' + ${cid}"

            String html = templateEngine.process("booking-confirmed", ctx);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (qrBytes != null && qrBytes.length > 0) {
                helper.addInline("qr", new ByteArrayResource(qrBytes), "image/png");
            }

            mailSender.send(msg);
        } catch (Exception e) {
            // log and swallow so notification flow is not disrupted
            System.err.println("[EmailService] Failed to send booking-confirmed email: " + e.getMessage());
        }
    }

    @Override
    public void sendNotificationEmail(Notification n) {
        var user = n.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        String to = user.getEmail();
        String subject = "[EVMS] " + safe(n.getTitle());
        String displayName = (user.getName() == null || user.getName().isBlank()) ? "bạn" : user.getName();

        sendNotificationEmailTpl(
                to,
                subject,
                displayName,
                n.getTitle(),
                n.getContentNoti(),
                n.getType(),
                n.getStatus(),
                n.getCreatedAt()
        );
    }

    // ========= 3) Booking confirmed với QR (CID) bằng Thymeleaf =========
    @Override
    public void sendHtmlWithInline(String to, String subject, String htmlIgnored, String cid, byte[] pngBytes) {
        try {
            // 1) Render template booking-confirmed.html
            Context ctx = new Context();
            // Các biến (có thể tuỳ caller set thêm nếu bạn mở rộng sign)
            ctx.setVariable("displayName", "bạn"); // tuỳ ý thay bằng tên thật ở nơi gọi nếu cần
            ctx.setVariable("title", "Đặt chỗ đã xác nhận");
            ctx.setVariable("body", "Vui lòng mang QR tới trạm để check-in");
            ctx.setVariable("cid", (cid != null && !cid.isBlank()) ? cid : "qr");

            String html = templateEngine.process("booking-confirmed", ctx);

            // 2) Tạo email multipart + attach inline
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8"); // multipart = true
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            String useCid = (cid != null && !cid.isBlank()) ? cid : "qr";
            if (pngBytes != null && pngBytes.length > 0) {
                helper.addInline(useCid, new ByteArrayResource(pngBytes), "image/png");
            }

            mailSender.send(msg);
        } catch (Exception e) {
            // Fallback: gửi text nếu render/gắn ảnh lỗi
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText("Không thể render HTML/QR lúc gửi email.", false);
                mailSender.send(msg);
            } catch (Exception ignored) {}
            // TODO: logger.error("sendHtmlWithInline failed", e);
        }
    }

    // ========= Helper =========
    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
