package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponse {
    private Long incidentId;
    private Long stationId;
    private String stationName;
    private Long staffId;
    private String staffName;
    private String title;
    private String description;
    private String severity;
    private ReportStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}
