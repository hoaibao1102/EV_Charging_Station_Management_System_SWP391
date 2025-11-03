package com.swp391.gr3.ev_management.DTO.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MoneyDto {
    private double amount;
    private String currency;
}
