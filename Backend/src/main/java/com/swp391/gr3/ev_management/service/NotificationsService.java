package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationsService {
    public List<Notification> findNotificationsByUserId(Long userId);
    public Notification createNotification(Notification notification);
    public Notification updateNotification(Notification notification);
    public void deleteNotification(Long notificationId);
    public Long getNotificationIdByUserId(Long userId);
    List<CreateNotificationResponse> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    List<CreateNotificationResponse> getNotificationsByUser(Long userId);
    List<CreateNotificationResponse> getUnreadNotificationsByUser(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
}
