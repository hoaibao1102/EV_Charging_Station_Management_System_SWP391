package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
    // Tìm payment method theo type
    List<PaymentMethod> findByMethodType(String methodType);

    // Tìm payment method theo provider
    List<PaymentMethod> findByProvider(String provider);

    // Tìm payment method theo type và provider
    Optional<PaymentMethod> findByMethodTypeAndProvider(PaymentType methodType, String provider);

    // Kiểm tra payment method có tồn tại không
    boolean existsByMethodTypeAndProvider(String methodType, String provider);
}
