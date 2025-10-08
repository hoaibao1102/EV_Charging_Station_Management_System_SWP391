package com.swp391.gr3.ev_management.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StopPointResponse {
    private Long pointId;
    private String pointNumber;
    private String status;
    private LocalDateTime updatedAt;
    private String message; // lời nhắn của staff khi trụ bảo trì hay lỗi gì đó
}
