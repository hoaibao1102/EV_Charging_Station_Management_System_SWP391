package com.swp391.gr3.ev_management.dto.response;

public record StaffTransactionStatsResponse(
        Long totalTransactions,
        Long completedTransactions,
        Long pendingTransactions,
        Long failedTransactions,
        Double totalRevenue
) {}