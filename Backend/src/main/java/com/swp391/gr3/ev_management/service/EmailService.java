package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface EmailService {
    void sendNotificationEmail(String to, String subject, String htmlBody);
    void sendNotificationEmail(Notification n);

    // CID inline
    void sendHtmlWithInline(String to, String subject, String html, String cid, byte[] pngBytes);

    // ✅ Mới: Gửi notification dùng Thymeleaf template "email-notification.html"
    void sendNotificationEmailTpl(String to,
                                  String subject,
                                  String displayName,
                                  Object title, Object body,
                                  Object type, Object status, Object createdAt);

    void sendBookingCancelledTpl(
            String to,
            String subject,
            String displayName,
            Long bookingId,
            String stationName,
            String timeRange
    );

    void sendBookingConfirmedTpl(String to,
                                 String subject,
                                 String displayName,
                                 Long bookingId,
                                 String station,
                                 String timeRange,
                                 String slotName,
                                 String connectorType,
                                 byte[] qrBytes);
}
