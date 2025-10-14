package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateNotificationResponse {
    private Long notificationId;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
