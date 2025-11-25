package com.swp391.gr3.ev_management.dto.response;

import com.swp391.gr3.ev_management.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DriverInvoiceDetail {

    private Long invoiceId;

    private Double amount;

    private String currency;

    private InvoiceStatus status;     // "PAID" | "UNPAID"

    private LocalDateTime issuedAt;

    private Long sessionId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double energyKWh;

    private Integer durationMinutes;

    private Integer initialSoc;

    private Integer finalSoc;

    private Double pricePerKWh;

    private Long bookingId;

    private Long stationId;

    private String stationName;

    private String pointNumber;

    private String vehiclePlate;
}
