package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.UnpaidInvoiceResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class UnpaidInvoiceMapper {
    public UnpaidInvoiceResponse mapToUnpaidInvoiceResponse(Invoice invoice) {
        ChargingSession session = invoice.getSession();
        Booking booking = session.getBooking();

        return UnpaidInvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .sessionId(session.getSessionId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(String.valueOf(invoice.getStatus()))
                .issuedAt(invoice.getIssuedAt())
                .stationName(booking.getStation().getStationName())
                .driverName(booking.getVehicle().getDriver().getUser().getName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .sessionStartTime(session.getStartTime())
                .sessionEndTime(session.getEndTime())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
