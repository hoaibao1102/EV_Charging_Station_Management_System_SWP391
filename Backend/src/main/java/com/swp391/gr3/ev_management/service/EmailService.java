package com.swp391.gr3.ev_management.service;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {

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
