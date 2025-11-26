package com.swp391.gr3.ev_management.mapper;

import com.swp391.gr3.ev_management.dto.response.ViewCharSessionResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.stereotype.Component;

@Component
public class ChargingSessionMapper {

    public ViewCharSessionResponse toResponse(ChargingSession cs) {
        Booking booking = cs.getBooking();

        // ===== SAFE GETTERS =====
        UserVehicle vehicle = (booking != null) ? booking.getVehicle() : null;

        String vehiclePlate =
                (vehicle != null) ? vehicle.getVehiclePlate() : "Unknown";

        Long vehicleId =
                (vehicle != null) ? vehicle.getVehicleId() : null;

        Long driverId =
                (vehicle != null && vehicle.getDriver() != null)
                        ? vehicle.getDriver().getDriverId()
                        : null;

        Long bookingId =
                (booking != null) ? booking.getBookingId() : null;

        String stationName =
                (booking != null && booking.getStation() != null)
                        ? booking.getStation().getStationName()
                        : "Unknown";

        return ViewCharSessionResponse.builder()
                .sessionId(cs.getSessionId())
                .bookingId(bookingId)
                .stationName(stationName)
                .vehiclePlate(vehiclePlate)
                .driverId(driverId)
                .startTime(cs.getStartTime())
                .endTime(cs.getEndTime())
                .energyKWh(cs.getEnergyKWh())
                .initialSoc(cs.getInitialSoc())
                .finalSoc(cs.getFinalSoc())
                .cost(cs.getCost())
                .currency("VND")          // hoặc lấy từ invoice/tariff nếu cần
                .status(cs.getStatus())
                .durationMinutes(cs.getDurationMinutes())
                .build();
    }
}
