package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    // Tìm booking theo ID và status (validate trước khi start session)
    Optional<Booking> findByBookingIdAndStatus(Long bookingId, String status);

    // Tìm tất cả booking của trạm
    List<Booking> findByStation_StationId(Long stationId);

    // Tìm booking theo status và trạm
    List<Booking> findByStation_StationIdAndStatus(Long stationId, String status);

    // Tìm booking confirmed sắp tới
    @Query("SELECT b FROM Booking b " +
            "WHERE b.station.stationId = :stationId " +
            "AND b.status = 'confirmed' " +
            "AND b.scheduledStartTime > :now " +
            "ORDER BY b.scheduledStartTime ASC")
    List<Booking> findUpcomingBookingsByStation(
            @Param("stationId") Long stationId,
            @Param("now") LocalDateTime now
    );

    // Tìm booking trong ngày hôm nay
    @Query("SELECT b FROM Booking b " +
            "WHERE b.station.stationId = :stationId " +
            "AND DATE(b.scheduledStartTime) = CURRENT_DATE " +
            "ORDER BY b.scheduledStartTime ASC")
    List<Booking> findTodayBookingsByStation(@Param("stationId") Long stationId);

    // Tìm booking theo khoảng thời gian
    @Query("SELECT b FROM Booking b " +
            "WHERE b.station.stationId = :stationId " +
            "AND b.scheduledStartTime >= :startDate " +
            "AND b.scheduledStartTime <= :endDate " +
            "ORDER BY b.scheduledStartTime ASC")
    List<Booking> findByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm booking theo status
    Long countByStation_StationIdAndStatus(Long stationId, String status);

    // Tìm booking theo vehicle plate (để staff search)
    @Query("SELECT b FROM Booking b " +
            "WHERE b.station.stationId = :stationId " +
            "AND b.vehicle.vehiclePlate LIKE %:plateNumber% " +
            "ORDER BY b.bookingTime DESC")
    List<Booking> findByStationAndVehiclePlate(
            @Param("stationId") Long stationId,
            @Param("plateNumber") String plateNumber
    );
}
