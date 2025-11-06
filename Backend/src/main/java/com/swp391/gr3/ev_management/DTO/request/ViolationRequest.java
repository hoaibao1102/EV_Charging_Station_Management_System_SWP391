package com.swp391.gr3.ev_management.DTO.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationRequest {
    @NotNull(message = "Description is required")
    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;

    @NotNull(message = "BookingId is required")
    private Long bookingId; // giữ đúng camelCase
}
