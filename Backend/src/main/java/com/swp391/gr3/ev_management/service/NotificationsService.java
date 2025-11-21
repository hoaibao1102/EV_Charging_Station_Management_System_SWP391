package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.dto.response.NotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationsService {
    void save(Notification noti);

    List<CreateNotificationResponse> getNotificationsByUser(Long userId);

    Long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    NotificationResponse getNotificationById(Long notificationId, Long userId);
}
