package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import com.swp391.gr3.ev_management.repository.PaymentMethodRepository;
import com.swp391.gr3.ev_management.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu lớp là Spring Service (chứa nghiệp vụ xử lý Transaction)
@RequiredArgsConstructor // Lombok tạo constructor cho các field final
@Slf4j // Thêm logger (log.info, log.error, ...)
public class TransactionServiceImpl implements TransactionService{

    // Repository thao tác DB cho bảng Transaction
    private final TransactionRepository transactionRepository;
    private final PaymentMethodService paymentMethodService;

    /**
     * Lưu transaction mới vào database.
     * - Dùng khi tạo giao dịch (vnpay / evm / staff xác nhận).
     */
    @Override
    public void addTransaction(Transaction transaction) {
        transactionRepository.save(transaction); // gọi JPA save()
    }

    /**
     * Lấy toàn bộ giao dịch của driver dựa trên userId.
     * - Hàm này dùng custom query sâu (deep graph) để fetch đầy đủ dữ liệu liên quan:
     *   driver → invoice → paymentMethod → session → booking → station...
     */
    @Override
    public List<TransactionBriefResponse> findAllDeepGraphByDriverUserId(Long userId) {
        return transactionRepository.findBriefByUserId(userId);
    }

    /**
     * Lưu và trả về transaction sau khi persist/update.
     * - Dùng khi service cần nhận lại entity đã lưu (ví dụ để trả về FE hoặc gán vào Notification).
     */
    @Override
    public Transaction save(Transaction tx) {
        return transactionRepository.save(tx); // save() trả về entity đã persist
    }

    /**
     * Tìm transaction theo ID.
     * - Dùng trong các flow xử lý callback VNPAY hoặc kiểm tra lịch sử.
     */
    @Override
    public Optional<Transaction> findById(Long transactionId) {
        return transactionRepository.findById(transactionId); // trả về Optional tránh lỗi null
    }

    @Override
    public double sumAmountByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Double sum = transactionRepository.sumAmountByCreatedAtBetween(start, end);
        return sum != null ? sum : 0.0;
    }

    @Override
    public List<Transaction> findTop5ByStatusOrderByCreatedAtDesc(TransactionStatus completed) {
        return transactionRepository.findTop5ByStatusOrderByCreatedAtDesc(completed);
    }

    @Override
    public List<TransactionBriefResponse> findBriefByUserId(Long userId) {
        return transactionRepository.findBriefByUserId(userId);
    }

    @Override
    public Optional<PaymentMethod> findByProvider(PaymentProvider evm) {
        return paymentMethodService.findByProvider(evm);
    }

}
