package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CompletedSessionView {
    private Long sessionId;
    private String connector;      // ct.displayName
    private String pointNumber;    // cp.pointNumber
    private String stationName;    // st.stationName
    private String driverName;     // u.name
    private String vehiclePlate;   // v.vehiclePlate
    private LocalDateTime startTime;
    private LocalDateTime endTime; // s.endTime (thực tế)
    private Double cost;           // s.cost (đã tính)
}
