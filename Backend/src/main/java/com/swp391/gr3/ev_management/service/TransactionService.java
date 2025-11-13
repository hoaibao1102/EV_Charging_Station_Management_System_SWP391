package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface TransactionService {
    void  addTransaction(Transaction transaction);

    List<Transaction> findAllDeepGraphByDriverUserId(Long userId);

    Transaction save(Transaction tx);

    Optional<Transaction> findById(Long transactionId);
}
