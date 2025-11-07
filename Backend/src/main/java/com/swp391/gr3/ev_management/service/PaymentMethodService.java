package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface PaymentMethodService {

    public PaymentMethod createPaymentMethod(PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate);

    public PaymentMethod updatePaymentMethod(Long methodId,
                                             PaymentType methodType,
                                             PaymentProvider provider,
                                             String accountNo,
                                             LocalDate expiryDate);

    List<PaymentMethodResponse> getAllPaymentMethods();
}
