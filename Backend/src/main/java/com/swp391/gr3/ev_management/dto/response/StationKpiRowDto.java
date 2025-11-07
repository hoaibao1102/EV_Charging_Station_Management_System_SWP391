package com.swp391.gr3.ev_management.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StationKpiRowDto {
    private Long stationId;
    private String stationName;

    private MoneyDto dayRevenue;
    private MoneyDto weekRevenue;
    private MoneyDto monthRevenue;
    private MoneyDto yearRevenue;

    private long sessions;
    private double utilization; // %
    private double growthPercent;
    private String status;      // "up"/"down" hoặc enum tuỳ bạn
}
