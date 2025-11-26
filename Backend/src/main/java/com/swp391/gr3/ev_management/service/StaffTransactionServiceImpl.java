package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.StaffTransactionStatsResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffTransactionServiceImpl implements StaffTransactionService {

    private final TransactionRepository transactionRepository;
    private final StationStaffService stationStaffService;   // ✅ thêm dependency này

    // ================== THEO STATION ==================

    @Override
    public Page<TransactionBriefResponse> getStationTransactions(Long stationId, Pageable pageable) {
        return transactionRepository.findByStationId(stationId, pageable);
    }

    @Override
    public Page<TransactionBriefResponse> getStationTransactionsByStatus(
            Long stationId,
            TransactionStatus status,
            Pageable pageable
    ) {
        return transactionRepository.findByStationIdAndStatus(stationId, status, pageable);
    }

    @Override
    public StaffTransactionStatsResponse getStationTransactionStats(Long stationId) {
        Long total     = transactionRepository.countByStationId(stationId);
        Long completed = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.COMPLETED);
        Long pending   = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.PENDING);
        Long failed    = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.FAILED);

        Double revenue = Optional.ofNullable(
                transactionRepository.sumAmountByStationIdAndStatus(stationId, TransactionStatus.COMPLETED)
        ).orElse(0.0);

        return new StaffTransactionStatsResponse(total, completed, pending, failed, revenue);
    }

    // ================== THEO STAFF (USER ID) ==================
    // Staff chỉ đưa userId, service sẽ tự map sang stationId và dùng các hàm ở trên.

    @Override
    public StaffTransactionStatsResponse getStaffTransactionStats(Long userId) {
        // 🔁 map userId (staff) -> stationId đang active
        Long stationId = stationStaffService.getStationIdByUserId(userId);
        return getStationTransactionStats(stationId);
    }

    @Override
    public Page<TransactionBriefResponse> getStaffTransactions(
            Long userId,
            TransactionStatus status,
            Pageable pageable
    ) {
        // 🔁 map userId (staff) -> stationId
        Long stationId = stationStaffService.getStationIdByUserId(userId);

        if (status == null) {
            return getStationTransactions(stationId, pageable);
        }
        return getStationTransactionsByStatus(stationId, status, pageable);
    }
}
