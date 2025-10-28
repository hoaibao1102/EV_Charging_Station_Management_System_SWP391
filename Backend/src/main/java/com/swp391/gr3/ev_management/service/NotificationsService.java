package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationsService {
    List<CreateNotificationResponse> getNotificationsByUser(Long userId);
    List<CreateNotificationResponse> getUnreadNotificationsByUser(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);

    NotificationResponse getNotificationById(Long notificationId, Long userId);
}
