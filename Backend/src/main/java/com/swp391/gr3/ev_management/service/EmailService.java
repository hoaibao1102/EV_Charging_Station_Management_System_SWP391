package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface EmailService {
    void sendNotificationEmail(String to, String subject, String htmlBody);
    void sendNotificationEmail(Notification n); // tiện dụng hơn
}
