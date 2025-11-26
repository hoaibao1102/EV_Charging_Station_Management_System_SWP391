package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.DriverInvoiceDetail;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DriverInvoiceMapper {

    public DriverInvoiceDetail toDto(
            Invoice invoice,
            Booking booking,
            ChargingPoint cp,
            Double pricePerKwh
    ) {
        return DriverInvoiceDetail.builder()
                .invoiceId(invoice.getInvoiceId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .issuedAt(invoice.getIssuedAt())
                .sessionId(invoice.getSession().getSessionId())
                .startTime(invoice.getSession().getStartTime())
                .endTime(invoice.getSession().getEndTime())
                .energyKWh(invoice.getSession().getEnergyKWh())
                .initialSoc(invoice.getSession().getInitialSoc())
                .finalSoc(invoice.getSession().getFinalSoc())
                .durationMinutes(
                        (int) Duration.between(
                                invoice.getSession().getStartTime(),
                                invoice.getSession().getEndTime()
                        ).toMinutes()
                )
                .bookingId(booking.getBookingId())
                .stationId(booking.getStation().getStationId())
                .stationName(booking.getStation().getStationName())
                .pointNumber(cp != null ? cp.getPointNumber() : null)   // 🔥 null-safe
                .vehiclePlate(
                        booking.getVehicle() != null
                                ? booking.getVehicle().getVehiclePlate()
                                : "Unknown"
                )
                .pricePerKWh(pricePerKwh)
                .build();
    }
}