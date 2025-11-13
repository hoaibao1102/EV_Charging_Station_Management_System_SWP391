package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public interface PaymentMethodService {

    PaymentMethodResponse createPaymentMethod(PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate);

    PaymentMethodResponse updatePaymentMethod(Long methodId,
                                             PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate);

    List<PaymentMethodResponse> getAllPaymentMethods();

    Optional<PaymentMethod> findById(Long paymentMethodId);

    Optional<PaymentMethod> findByMethodTypeAndProvider(@NotNull(message = "Payment method cannot be null") PaymentType paymentMethod, PaymentProvider paymentProvider);

    PaymentMethod save(PaymentMethod paymentMethod);
}
