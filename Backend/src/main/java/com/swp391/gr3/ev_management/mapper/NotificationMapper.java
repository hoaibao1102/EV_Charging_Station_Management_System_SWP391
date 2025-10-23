package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.CreateNotificationResponse;
import com.swp391.gr3.ev_management.DTO.response.NotificationResponse;
import com.swp391.gr3.ev_management.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public CreateNotificationResponse mapToResponse(Notification n) {
        return CreateNotificationResponse.builder()
                .notificationId(n.getNotiId())
                .userId(n.getUser().getUserId())
                .title(n.getTitle())
                .content(n.getContentNoti())
                .type(n.getType())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }

    public NotificationResponse mapToNotificationResponse(Notification n) {
        if (n == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(n.getNotiId())
                .userId(n.getUser() != null ? n.getUser().getUserId() : null)
                .title(n.getTitle())
                .content(n.getContentNoti())
                .type(n.getType())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
