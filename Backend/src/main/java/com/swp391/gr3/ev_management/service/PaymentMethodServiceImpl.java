package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.PaymentMethodMapper;
import com.swp391.gr3.ev_management.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu class này là 1 Spring Service xử lý nghiệp vụ PaymentMethod
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
public class PaymentMethodServiceImpl implements PaymentMethodService {

    // Repository thao tác DB (CRUD + các query custom)
    private final PaymentMethodRepository paymentMethodRepository;
    // Mapper convert Entity <-> DTO để trả về API
    private final PaymentMethodMapper paymentMethodMapper;

    /**
     * Tạo mới một PaymentMethod
     * - Kiểm tra trùng methodType + provider + accountNo
     * - Lưu entity
     * - Trả về DTO
     */
    @Override
    @Transactional // Cần transaction vì có thao tác ghi DB
    public PaymentMethodResponse createPaymentMethod(
            PaymentType methodType,
            PaymentProvider provider,
            String accountNo,
            LocalDate expiryDate
    ) {
        // 1️⃣ Kiểm tra trùng: nếu đã có cùng (methodType, provider, accountNo) → không cho tạo
        if (paymentMethodRepository
                .existsByMethodTypeAndProviderAndAccountNo(methodType, provider, accountNo)) {
            throw new ConflictException("PaymentMethod already exists with same type/provider/accountNo");
        }

        // 2️⃣ Khởi tạo entity PaymentMethod từ các tham số request
        PaymentMethod entity = PaymentMethod.builder()
                .methodType(methodType)
                .provider(provider)
                .accountNo(accountNo)
                .expiryDate(expiryDate)
                .build();

        // 3️⃣ Lưu xuống DB
        PaymentMethod saved = paymentMethodRepository.save(entity);

        // 4️⃣ Map sang DTO response để trả về cho client
        return paymentMethodMapper.toResponse(saved);
    }

    /**
     * Cập nhật thông tin PaymentMethod
     * - Tìm theo ID
     * - Update các field
     * - Lưu lại
     * - Trả về DTO
     */
    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(
            Long methodId,
            PaymentType methodType,
            PaymentProvider provider,
            String accountNo,
            LocalDate expiryDate
    ) {
        // 1️⃣ Tìm PaymentMethod theo methodId
        PaymentMethod existing = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ErrorException("PaymentMethod not found: " + methodId));

        // 2️⃣ Cập nhật các trường của PaymentMethod
        existing.setMethodType(methodType);
        existing.setProvider(provider);
        existing.setAccountNo(accountNo);
        existing.setExpiryDate(expiryDate);

        // 3️⃣ Lưu lại vào DB
        PaymentMethod updated = paymentMethodRepository.save(existing);

        // 4️⃣ Trả về DTO phản hồi
        return paymentMethodMapper.toResponse(updated);
    }

    /**
     * Lấy danh sách tất cả PaymentMethod trong hệ thống
     * - Gọi repo findAll()
     * - Map sang DTO
     */
    @Override
    public List<PaymentMethodResponse> getAllPaymentMethods() {
        return paymentMethodMapper.toResponseList(paymentMethodRepository.findAll());
    }

    /**
     * Tìm PaymentMethod theo ID
     * - Trả Optional để tránh lỗi null
     */
    @Override
    public Optional<PaymentMethod> findById(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId);
    }

    /**
     * Tìm PaymentMethod theo methodType + provider
     */
    @Override
    public Optional<PaymentMethod> findByMethodTypeAndProvider(PaymentType paymentMethod, PaymentProvider paymentProvider) {
        return paymentMethodRepository.findByMethodTypeAndProvider(paymentMethod, paymentProvider);
    }

    /**
     * Lưu trực tiếp PaymentMethod (dùng nội bộ)
     */
    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) {
        return paymentMethodRepository.save(paymentMethod);
    }

    @Override
    public Optional<PaymentMethod> findByProvider(PaymentProvider evm) {
        return paymentMethodRepository.findByProvider(evm);
    }
}
