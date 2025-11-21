package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public interface TransactionService {

    void  addTransaction(Transaction transaction);

    List<Transaction> findAllDeepGraphByDriverUserId(Long userId);

    Transaction save(Transaction tx);

    Optional<Transaction> findById(Long transactionId);

    double sumAmountByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    List<Transaction> findTop5ByStatusOrderByCreatedAtDesc(TransactionStatus completed);
}
