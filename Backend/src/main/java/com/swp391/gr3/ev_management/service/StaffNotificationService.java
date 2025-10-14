package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateNotificationRequest;
import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;

import java.util.List;

public interface StaffNotificationService {
    CreateNotificationResponse createNotification(CreateNotificationRequest request);
    List<CreateNotificationResponse> getNotificationsByUser(Long userId);
    List<CreateNotificationResponse> getUnreadNotificationsByUser(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
}
