package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNotificationResponse {
    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
