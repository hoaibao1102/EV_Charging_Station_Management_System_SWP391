package com.swp391.gr3.ev_management.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StopPointResponse {
    private Long pointId;
    private String pointNumber;
    private String stationName;
    private String status;
    private LocalDateTime updatedAt;
}
