package com.swp391.gr3.ev_management.DTO.response;

import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private Long methodId;
    private PaymentType methodType;
    private PaymentProvider provider;
    private String accountNo;
}
