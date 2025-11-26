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
                .pricePerKWh(tariff != null ? tariff.getPricePerKWh() : 0.0)
                .currency(tariff != null ? tariff.getCurrency() : null)
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

        String vehiclePlate =
                (booking.getVehicle() != null && booking.getVehicle().getVehiclePlate() != null)
                        ? booking.getVehicle().getVehiclePlate()
                        : "Unknown";

        String stationName =
                (booking.getStation() != null && booking.getStation().getStationName() != null)
                        ? booking.getStation().getStationName()
                        : "Unknown";

        String safePointNumber = (pointNumber != null) ? pointNumber : "Unknown";

        return StopCharSessionResponse.builder()
                .sessionId(cs.getSessionId())
                .stationName(stationName)
                .pointNumber(safePointNumber)
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
