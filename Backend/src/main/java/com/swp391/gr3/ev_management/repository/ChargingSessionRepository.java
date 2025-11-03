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

    // Tìm các session đang hoạt động (in_progress) tại trạm theo stationId
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.booking.station.stationId = :stationId " +
            "AND cs.status = 'in_progress'")
    List<ChargingSession> findActiveSessionsByStation(@Param("stationId") Long stationId);

    Optional<ChargingSession> findByBooking_BookingId(Long bookingId);

    @Query("""
    select cs
    from ChargingSession cs
    join fetch cs.booking b
    join fetch b.vehicle v
    join fetch v.driver d
    join fetch d.user u
    left join fetch b.bookingSlots bs
    left join fetch bs.slot s
    left join fetch s.chargingPoint cp
    left join fetch cp.connectorType ct
    where cs.sessionId = :sessionId
""")
    Optional<ChargingSession> findByIdWithBookingVehicleDriverUser(Long sessionId);

    @Query("""
        select s from ChargingSession s
          join fetch s.booking b
          join fetch b.vehicle v
          join fetch v.driver d
          join fetch d.user u
        where s.sessionId = :sid
    """)
    Optional<ChargingSession> findWithOwnerById(@Param("sid") Long sessionId);

    List<ChargingSession> findAllByBooking_Station_StationIdOrderByStartTimeDesc(Long stationId);

    @Query("SELECT COALESCE(SUM(s.energyKWh), 0) FROM ChargingSession s")
    double sumEnergyAll();

    @Query("SELECT COUNT(s) FROM ChargingSession s")
    long countAll();

    @Query("""
      SELECT COUNT(s)
      FROM ChargingSession s
      JOIN s.booking b
      WHERE b.station.stationId = :stationId
        AND s.startTime BETWEEN :from AND :to
    """)
    long countByStationBetween(@Param("stationId") Long stationId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(cs)
        FROM ChargingSession cs
        JOIN cs.booking b
        JOIN b.vehicle v
        JOIN v.driver d
        JOIN d.user u
        WHERE u.userId = :userId
    """)
    long countSessionsByUserId(@Param("userId") Long userId);
}
