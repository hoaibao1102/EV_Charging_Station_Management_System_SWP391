package com.swp391.gr3.ev_management.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsResponse {
    // Cards trên đầu
    private MoneyDto energyRevenueTotal;
    private double totalEnergyKWh;
    private long totalSessions;
    private MoneyDto avgRevenuePerSession;

    private MoneyDto dayRevenue;
    private MoneyDto weekRevenue;
    private MoneyDto monthRevenue;
    private MoneyDto yearRevenue;

    // Bảng theo trạm
    private List<StationKpiRowDto> stationRows;

    private LocalDateTime generatedAt;
}
