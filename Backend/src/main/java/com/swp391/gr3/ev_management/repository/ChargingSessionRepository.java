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

    // ✅ Tìm các phiên sạc đang hoạt động (status = 'in_progress') tại một trạm theo stationId
    //    Lọc theo khóa ngoại: cs.booking.station.stationId
    @Query("SELECT cs FROM ChargingSession cs " +
            "WHERE cs.booking.station.stationId = :stationId " +
            "AND cs.status = 'in_progress'")
    List<ChargingSession> findActiveSessionsByStation(@Param("stationId") Long stationId);

    // ✅ Tìm phiên sạc theo bookingId (dùng quan hệ lồng: ChargingSession.booking.bookingId)
    Optional<ChargingSession> findByBooking_BookingId(Long bookingId);

    // ✅ Lấy 1 phiên sạc kèm theo toàn bộ thông tin liên quan để hiển thị/logic:
    //    booking -> vehicle -> driver -> user
    //    + bookingSlots -> slot -> chargingPoint -> connectorType
    //    Dùng join fetch để tránh N+1 và LazyInitializationException
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

    // ✅ Lấy 1 phiên sạc và "chủ sở hữu" (user) của nó (đi sâu qua booking→vehicle→driver→user)
    //    Phù hợp để kiểm tra quyền sở hữu, hiển thị lịch sử, v.v.
    @Query("""
        select s from ChargingSession s
          join fetch s.booking b
          join fetch b.vehicle v
          join fetch v.driver d
          join fetch d.user u
        where s.sessionId = :sid
    """)
    Optional<ChargingSession> findWithOwnerById(@Param("sid") Long sessionId);

    // ✅ Lấy tất cả phiên sạc của một trạm (theo stationId) sắp xếp theo startTime giảm dần
    List<ChargingSession> findAllByBooking_Station_StationIdOrderByStartTimeDesc(Long stationId);

    // ✅ Tổng năng lượng (kWh) đã sạc của toàn hệ thống (COALESCE để tránh null)
    @Query("SELECT COALESCE(SUM(s.energyKWh), 0) FROM ChargingSession s")
    double sumEnergyAll();

    // ✅ Tổng số phiên sạc trong hệ thống
    @Query("SELECT COUNT(s) FROM ChargingSession s")
    long countAll();

    // ✅ Đếm số phiên sạc tại một trạm trong khoảng thời gian [from, to] dựa trên startTime
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

    // ✅ Đếm số phiên sạc gắn với một người dùng (userId) cụ thể
    //    Đi sâu qua quan hệ: session → booking → vehicle → driver → user
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

    // ✅ Lấy toàn bộ phiên sạc của một user (theo userId) kèm thông tin liên quan (fetch sâu)
    //    Sắp xếp theo startTime desc, rồi createdAt desc (ổn định thứ tự)
    @Query("""
       select s
       from ChargingSession s
         join fetch s.booking b
         join fetch b.vehicle v
         join fetch v.driver d
         join fetch d.user u
       where u.userId = :userId
       order by s.startTime desc, s.createdAt desc
       """)
    List<ChargingSession> findAllByDriverUserIdDeep(@Param("userId") Long userId);

    /**
     * ✅ Lấy tất cả session gắn với một ChargingPoint cụ thể (qua Booking -> BookingSlot -> Slot -> ChargingPoint).
     * - Dùng DISTINCT để tránh trùng do join nhiều bảng.
     * - FETCH JOIN sâu để tránh N+1 (lấy luôn các quan hệ cần hiển thị).
     * - Sắp xếp session mới nhất lên trước.
     */
    @Query("""
        select distinct cs
        from ChargingSession cs
          join fetch cs.booking b
          join fetch b.vehicle v
          join fetch v.driver d
          join fetch d.user u
          left join fetch b.bookingSlots bs
          left join fetch bs.slot s
          left join fetch s.chargingPoint cp
        where cp.pointId = :pointId
        order by cs.startTime desc, cs.createdAt desc
    """)
    List<ChargingSession> findAllByChargingPointIdDeep(@Param("pointId") Long pointId);
}
