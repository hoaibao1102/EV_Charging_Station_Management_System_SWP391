package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.*;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class DashboardStatsMapper {

    public DashboardStatsResponse toDashboardStatsResponse(
            double totalRevenue,
            double totalEnergy,
            long totalSessions,
            double avgPerSession,
            double dayRevenue,
            double weekRevenue,
            double monthRevenue,
            double yearRevenue,
            List<StationKpiRowDto> stationRows,
            String baseCurrency
    ) {
        return DashboardStatsResponse.builder()
                .energyRevenueTotal(money(totalRevenue, baseCurrency))
                .totalEnergyKWh(totalEnergy)
                .totalSessions(totalSessions)
                .avgRevenuePerSession(money(avgPerSession, baseCurrency))
                .dayRevenue(money(dayRevenue, baseCurrency))
                .weekRevenue(money(weekRevenue, baseCurrency))
                .monthRevenue(money(monthRevenue, baseCurrency))
                .yearRevenue(money(yearRevenue, baseCurrency))
                .stationRows(stationRows)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private MoneyDto money(double amount, String currency) {
        return MoneyDto.builder()
                .amount(amount)
                .currency(currency)
                .build();
    }
}
