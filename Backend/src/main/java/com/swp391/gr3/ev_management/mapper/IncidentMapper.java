package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.IncidentResponse;
import com.swp391.gr3.ev_management.entity.Incident;
import com.swp391.gr3.ev_management.entity.StationStaff;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public IncidentResponse mapToIncident(Incident i) {
        StationStaff s = new StationStaff();
        return IncidentResponse.builder()
                .incidentId(i.getIncidentId())
                .stationId(s.getStation().getStationId())
                .stationName(s.getStation().getStationName())
                .staffId(s.getUser().getUserId())
                .staffName(s.getUser().getName())
                .title(i.getTitle())
                .description(i.getDescription())
                .severity(i.getSeverity())
                .status(i.getStatus())
                .reportedAt(i.getReportedAt())
                .resolvedAt(i.getResolvedAt())
                .build();
    }
}
