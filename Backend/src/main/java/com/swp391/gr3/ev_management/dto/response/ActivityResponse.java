package com.swp391.gr3.ev_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivityResponse {

    private Long id;                   // ID của activity (map theo entity ID)
    private String type;               // SESSION_START / BOOKING_NEW / PAYMENT_SUCCESS
    private String description;        // mô tả hiển thị
    private LocalDateTime timestamp;   // thời gian xảy ra
    private String relatedEntityType;  // SESSION / BOOKING / TRANSACTION
    private Long relatedEntityId;      // ID của entity liên quan (sessionId, bookingId, transactionId)
}
