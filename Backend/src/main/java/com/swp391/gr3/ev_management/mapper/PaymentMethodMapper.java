package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.PaymentMethodResponse;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMethodMapper {

    public PaymentMethodResponse toResponse(PaymentMethod pm) {
        if (pm == null) return null;
        return PaymentMethodResponse.builder()
                .methodId(pm.getMethodId())
                .methodType(pm.getMethodType())
                .provider(pm.getProvider())
                .accountNo(pm.getAccountNo())
                .build();
    }

    public List<PaymentMethodResponse> toResponseList(List<PaymentMethod> list) {
        return list.stream().map(this::toResponse).toList();
    }
}
