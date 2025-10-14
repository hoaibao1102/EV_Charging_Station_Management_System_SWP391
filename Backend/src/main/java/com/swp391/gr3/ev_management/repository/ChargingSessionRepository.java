package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSession,Long> {
    // Tìm session theo status
    List<ChargingSession> findByStatus(String status);
    // Tìm session theo bookingId
    Optional<ChargingSession> findByBooking_BookingId(Long bookingId);
    // Tìm session theo status
    List<ChargingSession> findByBooking_Station_StationId(Long stationId);
    // Kiểm tra tồn tại session theo bookingId và status
    boolean existsByBooking_BookingIdAndStatus(Long bookingId, String status);

    // Tìm các session đang hoạt động (in_progress) tại trạm theo stationId
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.booking.station.stationId = :stationId " +
            "AND cs.status = 'in_progress'")
    List<ChargingSession> findActiveSessionsByStation(@Param("stationId") Long stationId);

    // Tìm session theo khoảng thời gian
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.booking.station.stationId = :stationId " +
            "AND cs.startTime >= :startDate " +
            "AND cs.startTime <= :endDate " +
            "ORDER BY cs.startTime DESC")
    List<ChargingSession> findByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm số session active tại trạm
    @Query("SELECT COUNT(cs) FROM ChargingSession cs " +
            "WHERE cs.booking.station.stationId = :stationId " +
            "AND cs.status = 'in_progress'")
    Long countActiveSessionsByStation(@Param("stationId") Long stationId);

    // Tìm session completed hôm nay của trạm
//    @Query("SELECT cs FROM ChargingSession cs " +
//            "WHERE cs.booking.station.stationId = :stationId " +
//            "AND LOWER(cs.status) = 'completed' " +
//            "AND cs.endTime >= :startOfDay " +
//            "AND cs.endTime < :endOfDay " +
//            "ORDER BY cs.endTime DESC")
//    List<ChargingSession> findCompletedSessionsTodayByStation(
//            @Param("stationId") Long stationId,
//            @Param("startOfDay") LocalDateTime startOfDay,
//            @Param("endOfDay") LocalDateTime endOfDay);

    // Tìm session theo driver ID
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.booking.vehicle.driver.driverId = :driverId " +
            "ORDER BY cs.startTime DESC")
    List<ChargingSession> findByDriverId(@Param("driverId") Long driverId);
}
