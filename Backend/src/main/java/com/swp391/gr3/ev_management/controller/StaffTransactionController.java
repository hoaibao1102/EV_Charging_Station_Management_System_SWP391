package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.StaffTransactionStatsResponse;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.service.StaffTransactionService;
import com.swp391.gr3.ev_management.service.StationStaffService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/transactions")
@Tag(name = "Staff Transaction Management", description = "APIs for staff to manage their station's transactions")
public class StaffTransactionController {

    @Autowired
    private StaffTransactionService staffTransactionService;

    @Autowired
    private StationStaffService stationStaffService;

    @Autowired
    private TokenService tokenService;

    // ✅ Lấy danh sách giao dịch của trạm (có phân trang, filter, sort)
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping
    @Operation(summary = "Get station transactions", description = "Staff retrieves all transactions of their assigned station")
    public ResponseEntity<Page<TransactionBriefResponse>> getStationTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // 🔑 Lấy userId từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🏢 Lấy stationId của staff
        Long stationId = stationStaffService.getStationIdByUserId(userId);

        // 📄 Tạo Pageable
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 🔍 Lấy giao dịch
        Page<TransactionBriefResponse> transactions;
        if (status != null) {
            transactions = staffTransactionService.getStationTransactionsByStatus(stationId, status, pageable);
        } else {
            transactions = staffTransactionService.getStationTransactions(stationId, pageable);
        }
        return ResponseEntity.ok(transactions);
    }

    // ✅ Lấy thống kê giao dịch của trạm
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/stats")
    @Operation(summary = "Get station transaction statistics", description = "Staff retrieves transaction statistics of their assigned station")
    public ResponseEntity<StaffTransactionStatsResponse> getStationStats(HttpServletRequest request) {
        // 🔑 Lấy userId từ token
        Long userId = tokenService.extractUserIdFromRequest(request);

        // 🏢 Lấy stationId của staff
        Long stationId = stationStaffService.getStationIdByUserId(userId);

        // 📊 Lấy thống kê
        StaffTransactionStatsResponse stats = staffTransactionService.getStationTransactionStats(stationId);

        return ResponseEntity.ok(stats);
    }
}