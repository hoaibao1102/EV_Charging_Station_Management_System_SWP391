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

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService{

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    /**
     * Tạo mới một PaymentMethod
     */
    @Override
    @Transactional
    public PaymentMethodResponse createPaymentMethod(
            PaymentType methodType,
            PaymentProvider provider,
            String accountNo,
            LocalDate expiryDate
    ) {
        if (paymentMethodRepository
                .existsByMethodTypeAndProviderAndAccountNo(methodType, provider, accountNo)) {
            throw new ConflictException("PaymentMethod already exists with same type/provider/accountNo");
        }

        PaymentMethod entity = PaymentMethod.builder()
                .methodType(methodType)
                .provider(provider)
                .accountNo(accountNo)
                .expiryDate(expiryDate)
                .build();

        PaymentMethod saved = paymentMethodRepository.save(entity);
        return paymentMethodMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(
            Long methodId,
            PaymentType methodType,
            PaymentProvider provider,
            String accountNo,
            LocalDate expiryDate
    ) {
        PaymentMethod existing = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ErrorException("PaymentMethod not found: " + methodId));

        // ✅ Cập nhật các field
        existing.setMethodType(methodType);
        existing.setProvider(provider);
        existing.setAccountNo(accountNo);
        existing.setExpiryDate(expiryDate);

        PaymentMethod updated = paymentMethodRepository.save(existing);
        return paymentMethodMapper.toResponse(updated);
    }

    @Override
    public List<PaymentMethodResponse> getAllPaymentMethods() {
        return paymentMethodMapper.toResponseList(paymentMethodRepository.findAll());
    }
}
