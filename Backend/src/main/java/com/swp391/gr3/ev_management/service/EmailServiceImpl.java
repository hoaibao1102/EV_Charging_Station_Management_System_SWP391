package com.swp391.gr3.ev_management.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf

    @Value("${app.notifications.email.from:no-reply@evms.local}")
    private String from;

    // ========= 1) Gửi email HTML đã render sẵn (legacy-compatible) =========

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
            log.error("sendNotificationEmailTpl failed", e);
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

    // ========= Helper =========
    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
