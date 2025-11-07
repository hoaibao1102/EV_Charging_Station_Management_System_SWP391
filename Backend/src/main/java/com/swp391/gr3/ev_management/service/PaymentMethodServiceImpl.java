package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
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

    /**
     * Tạo mới một PaymentMethod
     */
    @Override
    @Transactional
    public PaymentMethod createPaymentMethod(PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate) {

        boolean exists = paymentMethodRepository
                .existsByMethodTypeAndProviderAndAccountNo(methodType, provider, accountNo);
        if (exists) {
            throw new ConflictException("PaymentMethod already exists with same type/provider/accountNo");
        }

        PaymentMethod pm = PaymentMethod.builder()
                .methodType(methodType)
                .provider(provider)
                .accountNo(accountNo)
                .expiryDate(expiryDate)
                .build();

        return paymentMethodRepository.save(pm);
    }

    @Override
    @Transactional
    public PaymentMethod updatePaymentMethod(Long methodId,
                                             PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate) {
        PaymentMethod existing = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ErrorException("PaymentMethod not found: " + methodId));

        existing.setMethodType(methodType);
        existing.setProvider(provider);
        existing.setAccountNo(accountNo);
        existing.setExpiryDate(expiryDate);

        return paymentMethodRepository.save(existing);
    }

    @Override
    public List<PaymentMethodResponse> getAllPaymentMethods() {
        return paymentMethodRepository.findAll().stream().map(pm -> PaymentMethodResponse.builder()
                .methodId(pm.getMethodId())
                .methodType(pm.getMethodType())
                .provider(pm.getProvider())
                .accountNo(pm.getAccountNo())
                .build()).toList();
    }
}
