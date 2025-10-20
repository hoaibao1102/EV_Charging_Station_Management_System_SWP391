package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.IncidentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {
    private Long incidentId;
    private Long stationId;
    private String stationName;
    private Long staffId;
    private String staffName;
    private String title;
    private String description;
    private String severity;
    private IncidentStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}
