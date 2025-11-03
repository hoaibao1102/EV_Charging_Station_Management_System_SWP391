package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.response.DashboardStatsResponse;
import com.swp391.gr3.ev_management.DTO.response.UserTotalsResponse;
import org.springframework.stereotype.Service;

@Service
public interface StatisticsService {

    DashboardStatsResponse getDashboard();

    UserTotalsResponse getTotals();
}
