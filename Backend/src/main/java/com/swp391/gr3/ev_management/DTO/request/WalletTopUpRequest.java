package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class WalletTopUpRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1000.0", message = "Minimum top-up amount is 1000 VND")
        private BigDecimal amount;

        private String paymentMethod; // Optional: MoMo, Bank Transfer, etc.

}
