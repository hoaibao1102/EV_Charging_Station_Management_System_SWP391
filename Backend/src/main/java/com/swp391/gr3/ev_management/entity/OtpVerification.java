package com.swp391.gr3.ev_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "otp_verification")
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email; // hoặc phoneNumber nếu bạn gửi OTP qua SMS
    private String otpCode;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private boolean verified;
}
