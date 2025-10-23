package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
        Long id,
        Long userId,
        NotificationTypes type,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotiId(),
                n.getUser() != null ? n.getUser().getUserId() : null,
                n.getType(),
                n.getTitle(),
                n.getContentNoti(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
