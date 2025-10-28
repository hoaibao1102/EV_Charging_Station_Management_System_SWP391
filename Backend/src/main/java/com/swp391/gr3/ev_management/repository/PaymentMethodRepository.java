package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
    // Tìm payment method theo type và provider
    Optional<PaymentMethod> findByMethodTypeAndProvider(PaymentType methodType, PaymentProvider provider);

    boolean existsByMethodTypeAndProviderAndAccountNo(
            PaymentType methodType, PaymentProvider provider, String accountNo
    );
}
