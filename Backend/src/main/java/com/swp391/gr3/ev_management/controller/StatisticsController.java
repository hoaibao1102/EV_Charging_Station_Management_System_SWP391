// src/main/java/com/swp391/gr3/ev_management/controller/StatisticsController.java
package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.response.DashboardStatsResponse;
import com.swp391.gr3.ev_management.DTO.response.UserTotalsResponse;
import com.swp391.gr3.ev_management.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboard() {
        DashboardStatsResponse body = statisticsService.getDashboard();
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/totals")
    public ResponseEntity<UserTotalsResponse> getTotals() {
        return ResponseEntity.ok(statisticsService.getTotals());
    }
}
