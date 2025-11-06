package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.ChargingSessionBriefResponse;
import com.swp391.gr3.ev_management.DTO.response.TransactionBriefResponse;
import com.swp391.gr3.ev_management.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class DriverDataMapper {

    // ================== MAPPER: Transaction ==================
    public static TransactionBriefResponse toTransactionBriefResponse(Transaction t) {
        if (t == null) return null;

        Invoice i = t.getInvoice();
        ChargingSession s = (i != null) ? i.getSession() : null;
        Booking b = (s != null) ? s.getBooking() : null;
        UserVehicle v = (b != null) ? b.getVehicle() : null;
        ChargingStation st = (b != null) ? b.getStation() : null;

        return TransactionBriefResponse.builder()
                .transactionId(t.getTransactionId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())

                .invoiceId(i != null ? i.getInvoiceId() : null)
                .sessionId(s != null ? s.getSessionId() : null)
                .bookingId(b != null ? b.getBookingId() : null)

                .stationId(st != null ? st.getStationId() : null)
                .stationName(st != null ? st.getStationName() : null)
                .vehicleId(v != null ? v.getVehicleId() : null)
                .vehiclePlate(v != null ? v.getVehiclePlate() : null)
                .build();
    }

    public static List<TransactionBriefResponse> toTransactionBriefResponseList(List<Transaction> txs) {
        return txs.stream()
                .map(DriverDataMapper::toTransactionBriefResponse)
                .collect(Collectors.toList());
    }

    // ================== MAPPER: ChargingSession ==================
    public static ChargingSessionBriefResponse toChargingSessionBriefResponse(ChargingSession s) {
        if (s == null) return null;

        Booking b = s.getBooking();
        UserVehicle v = (b != null) ? b.getVehicle() : null;
        ChargingStation st = (b != null) ? b.getStation() : null;

        return ChargingSessionBriefResponse.builder()
                .sessionId(s.getSessionId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .initialSoc(s.getInitialSoc())
                .finalSoc(s.getFinalSoc())
                .energyKWh(s.getEnergyKWh())
                .durationMinutes(s.getDurationMinutes())
                .cost(s.getCost())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())

                .bookingId(b != null ? b.getBookingId() : null)
                .stationId(st != null ? st.getStationId() : null)
                .stationName(st != null ? st.getStationName() : null)
                .vehicleId(v != null ? v.getVehicleId() : null)
                .vehiclePlate(v != null ? v.getVehiclePlate() : null)

                .invoiceId(s.getInvoice() != null ? s.getInvoice().getInvoiceId() : null)
                .build();
    }

    public static List<ChargingSessionBriefResponse> toChargingSessionBriefResponseList(List<ChargingSession> sessions) {
        return sessions.stream()
                .map(DriverDataMapper::toChargingSessionBriefResponse)
                .collect(Collectors.toList());
    }
}
