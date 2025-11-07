// src/main/java/com/swp391/gr3/ev_management/controller/StatisticsController.java
package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.DashboardStatsResponse;
import com.swp391.gr3.ev_management.dto.response.UserTotalsResponse;
import com.swp391.gr3.ev_management.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController // ✅ Đánh dấu đây là REST Controller — trả dữ liệu JSON thay vì view
@RequestMapping("/api/statics") // ✅ Tất cả endpoint trong controller này bắt đầu bằng /api/statics
@RequiredArgsConstructor // ✅ Lombok: tự động tạo constructor cho các field final (Dependency Injection)
public class StatisticsController {

    private final StatisticsService statisticsService; // ✅ Service xử lý logic liên quan đến thống kê dữ liệu hệ thống

    // =========================================================================
    // ✅ 1. ADMIN: LẤY THỐNG KÊ TỔNG QUAN (DASHBOARD)
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ người dùng có vai trò ADMIN mới được truy cập endpoint này
    @GetMapping("/dashboard") // 🔗 Endpoint: GET /api/statics/dashboard
    public ResponseEntity<DashboardStatsResponse> getDashboard() {
        // 🟢 Gọi service để lấy dữ liệu thống kê tổng quan cho dashboard admin
        //    Ví dụ: số lượng người dùng, trạm, doanh thu, lượt sạc, ...
        DashboardStatsResponse body = statisticsService.getDashboard();

        // 🟢 Trả về HTTP 200 (OK) cùng dữ liệu thống kê dạng JSON
        return ResponseEntity.ok(body);
    }

    // =========================================================================
    // ✅ 2. ADMIN: LẤY TỔNG SỐ LIỆU NGƯỜI DÙNG (USER TOTALS)
    // =========================================================================
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Chỉ ADMIN có quyền xem tổng số người dùng
    @GetMapping("/totals") // 🔗 Endpoint: GET /api/statics/totals
    public ResponseEntity<UserTotalsResponse> getTotals() {
        // 🟢 Gọi service để lấy tổng số liệu người dùng theo từng loại (admin, staff, driver, ...)
        UserTotalsResponse response = statisticsService.getTotals();

        // 🟢 Trả về HTTP 200 (OK) cùng dữ liệu tổng hợp
        return ResponseEntity.ok(response);
    }
}
