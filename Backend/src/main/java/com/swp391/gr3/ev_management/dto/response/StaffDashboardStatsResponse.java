package com.swp391.gr3.ev_management.dto.response;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardStatsResponse {
    private long activeStations;
    private long activeSessions;
    private long todayBookings;
    private double todayRevenue;
    private Map<String, Long> pointStats;
}
