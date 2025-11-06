package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.ReportResponse;
import com.swp391.gr3.ev_management.entity.Report;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportResponse mapToReport(Report i) {
        var station = i.getStation();             // luôn đúng kiểu ChargingStation
        var staff   = i.getStaffs();              // kiểu Staffs

        Long stationId   = (station != null) ? station.getStationId()   : null;
        String stationName = (station != null) ? station.getStationName() : null;

        Long staffId     = (staff != null) ? staff.getStaffId() : null; // 👈 đúng
        String staffName = (staff != null && staff.getUser() != null)
                ? staff.getUser().getName()
                : null;

        return ReportResponse.builder()
                .incidentId(i.getIncidentId())
                .stationId(stationId)
                .stationName(stationName)
                .staffId(staffId)
                .staffName(staffName)
                .title(i.getTitle())
                .description(i.getDescription())
                .severity(i.getSeverity())
                .status(i.getStatus())
                .reportedAt(i.getReportedAt())
                .resolvedAt(i.getResolvedAt())
                .build();
    }
}
