package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.StaffTransactionStatsResponse;
import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffTransactionServiceImpl implements StaffTransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public Page<TransactionBriefResponse> getStationTransactions(Long stationId, Pageable pageable) {
        return transactionRepository.findByStationId(stationId, pageable);
    }

    @Override
    public Page<TransactionBriefResponse> getStationTransactionsByStatus(Long stationId, TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStationIdAndStatus(stationId, status, pageable);
    }

    @Override
    public StaffTransactionStatsResponse getStationTransactionStats(Long stationId) {
        Long total = transactionRepository.countByStationId(stationId);
        Long completed = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.COMPLETED);
        Long pending = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.PENDING);
        Long failed = transactionRepository.countByStationIdAndStatus(stationId, TransactionStatus.FAILED);
        Double revenue = transactionRepository.sumAmountByStationIdAndStatus(stationId, TransactionStatus.COMPLETED);

        return new StaffTransactionStatsResponse(total, completed, pending, failed, revenue);
    }
}
