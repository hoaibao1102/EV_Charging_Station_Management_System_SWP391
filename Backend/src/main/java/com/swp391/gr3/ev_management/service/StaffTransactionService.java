package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.dto.response.StaffTransactionStatsResponse;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffTransactionService {
    // Lấy danh sách giao dịch của trạm (có phân trang)
    Page<TransactionBriefResponse> getStationTransactions(Long stationId, Pageable pageable);

    // Lấy danh sách giao dịch theo status
    Page<TransactionBriefResponse> getStationTransactionsByStatus(Long stationId, TransactionStatus status, Pageable pageable);

    // Lấy thống kê giao dịch của trạm
    StaffTransactionStatsResponse getStationTransactionStats(Long stationId);
}