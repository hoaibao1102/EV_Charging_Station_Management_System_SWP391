package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.StopCharSessionResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.entity.Tariff;
import org.springframework.stereotype.Component;

@Component
public class StopCharSessionResponseMapper {

    public StopCharSessionResponse mapWithTariff(
            ChargingSession cs,
            Booking booking,
            String pointNumber,
            Tariff tariff
    ) {
        return base(cs, booking, pointNumber)
                .pricePerKWh(tariff.getPricePerKWh())
                .currency(tariff.getCurrency())
                .build();
    }

    public StopCharSessionResponse mapNoBilling(
            ChargingSession cs,
            Booking booking,
            String pointNumber
    ) {
        return base(cs, booking, pointNumber)
                .pricePerKWh(0.0)
                .currency(null)
                .build();
    }

    private StopCharSessionResponse.StopCharSessionResponseBuilder base(
            ChargingSession cs,
            Booking booking,
            String pointNumber
    ) {

        // ===== SAFE GETTER FOR VEHICLE =====
        String vehiclePlate =
                (booking.getVehicle() != null)
                        ? booking.getVehicle().getVehiclePlate()
                        : "Unknown";

        return StopCharSessionResponse.builder()
                .sessionId(cs.getSessionId())
                .stationName(booking.getStation().getStationName())
                .pointNumber(pointNumber)
                .vehiclePlate(vehiclePlate)
                .startTime(cs.getStartTime())
                .endTime(cs.getEndTime())
                .durationMinutes(cs.getDurationMinutes())
                .energyKWh(cs.getEnergyKWh())
                .cost(cs.getCost())
                .status(cs.getStatus())
                .initialSoc(cs.getInitialSoc())
                .finalSoc(cs.getFinalSoc());
    }
}
