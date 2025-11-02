package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.DTO.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import org.springframework.stereotype.Component;

@Component
public class ChargingSessionMapper {
    public ViewCharSessionResponse toResponse(ChargingSession session) {
        Booking booking = session.getBooking();

        return ViewCharSessionResponse.builder()
                .sessionId(session.getSessionId())
                .bookingId(booking.getBookingId())
                .stationName(booking.getStation().getStationName())
                .vehiclePlate(booking.getVehicle().getVehiclePlate())
                .driverId(booking.getVehicle().getDriver().getDriverId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .energyKWh(session.getEnergyKWh())
                .initialSoc(session.getInitialSoc())
                .cost(session.getCost())
                .currency("VND")
                .status(session.getStatus())
                .durationMinutes(session.getDurationMinutes())
                .build();
    }
}
