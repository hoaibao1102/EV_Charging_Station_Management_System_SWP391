package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.MoneyDto;
import com.swp391.gr3.ev_management.dto.response.StationKpiRowDto;
import com.swp391.gr3.ev_management.dto.response.UserTotalsResponse;
import org.springframework.stereotype.Component;

@Component
public class StatisticsResponseMapper {

    public UserTotalsResponse toUserTotalsResponse(
            long totalUsers, long totalDrivers, long activeDrivers,
            long totalStaffs, long activeStaffs
    ) {
        return UserTotalsResponse.builder()
                .totalUsers(totalUsers)
                .totalDrivers(totalDrivers)
                .activeDrivers(activeDrivers)
                .totalStaffs(totalStaffs)
                .activeStaffs(activeStaffs)
                .build();
    }

    /* -------- helpers -------- */
    private MoneyDto money(double amount, String currency) {
        return MoneyDto.builder()
                .amount(amount)
                .currency(currency)
                .build();
    }
}
