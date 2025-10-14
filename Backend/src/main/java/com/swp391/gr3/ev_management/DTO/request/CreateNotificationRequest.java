package com.swp391.gr3.ev_management.DTO.request;

import lombok.Data;

@Data
public class CreateNotificationRequest {
    private Long userId;
    private String type;
    private String title;
    private String content;
}
