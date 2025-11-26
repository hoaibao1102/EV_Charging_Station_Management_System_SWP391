package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.dto.response.ActiveSessionView;
import com.swp391.gr3.ev_management.dto.response.CompletedSessionView;
import com.swp391.gr3.ev_management.entity.ChargingSession;
import com.swp391.gr3.ev_management.enums.ChargingSessionStatus;
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
        left join fetch cs.booking b
        left join fetch b.vehicle v
        left join fetch v.driver d
        left join fetch d.user u
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
      left join fetch s.booking b
      left join fetch b.vehicle v
      left join fetch v.driver d
      left join fetch d.user u
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

    /**
     * ✅ Danh sách phiên sạc đang hoạt động, nhưng CHỈ ở các trạm mà staff (staffId) đang được assign.
     * - DISTINCT để tránh nhân bản do Booking có nhiều BookingSlot → nhiều ChargingPoint.
     * - EXISTS kiểm tra nhân viên này có assignment active ở trạm của phiên sạc:
     *   + ss.station = st
     *   + ss.staff.staffId = :staffId
     *   + ss.assignedAt <= CURRENT_TIMESTAMP
     *   + (ss.unassignedAt IS NULL OR ss.unassignedAt > CURRENT_TIMESTAMP)
     */
    @Query("""
        select distinct new com.swp391.gr3.ev_management.dto.response.ActiveSessionView(
            s.sessionId,
            ct.displayName,
            cp.pointNumber,
            st.stationName,
            u.name,
            v.vehiclePlate,
            s.startTime,
            b.scheduledEndTime
        )
        from ChargingSession s
            join s.booking b
            join b.vehicle v
            join v.model vm
            join vm.connectorType ct
            join b.bookingSlots bs
            join bs.slot sl
            join sl.chargingPoint cp
            join cp.station st
            join v.driver d
            join d.user u
        where s.status = com.swp391.gr3.ev_management.enums.ChargingSessionStatus.IN_PROGRESS
          and exists (
                select 1
                from StationStaff ss
                where ss.station = st
                  and ss.staff.staffId = :staffId
                  and ss.assignedAt <= CURRENT_TIMESTAMP
                  and (ss.unassignedAt is null or ss.unassignedAt > CURRENT_TIMESTAMP)
          )
        order by s.startTime desc
    """)
    List<ActiveSessionView> findActiveSessionCompactByStaff(@Param("staffId") Long staffId);

    /**
     * ✅ Danh sách phiên sạc ĐÃ KẾT THÚC (COMPLETED), CHỈ ở các trạm mà staff (staffId) đang được assign.
     * - DISTINCT để tránh nhân bản do Booking có nhiều BookingSlot.
     * - EXISTS bảo đảm staff này đang active ở trạm của phiên sạc.
     * - Lấy thêm s.cost và s.endTime (thời gian kết thúc thực tế).
     * - Sắp xếp theo endTime mới nhất trước.
     */
    @Query("""
    select distinct new com.swp391.gr3.ev_management.dto.response.CompletedSessionView(
        s.sessionId,
        ct.displayName,
        cp.pointNumber,
        st.stationName,
        u.name,
        v.vehiclePlate,
        s.startTime,
        s.endTime,
        s.cost
    )
        from ChargingSession s
            join s.booking b
            join b.vehicle v
            join v.model vm
            join vm.connectorType ct
            join b.bookingSlots bs
            join bs.slot sl
            join sl.chargingPoint cp
            join cp.station st
            join v.driver d
            join d.user u
        where s.status = com.swp391.gr3.ev_management.enums.ChargingSessionStatus.COMPLETED
          and exists (
                select 1
                from StationStaff ss
                where ss.station = st
                  and ss.staff.staffId = :staffId
                  and ss.assignedAt <= CURRENT_TIMESTAMP
                  and (ss.unassignedAt is null or ss.unassignedAt > CURRENT_TIMESTAMP)
          )
        order by s.endTime desc
    """)
    List<CompletedSessionView> findCompletedSessionCompactByStaff(@Param("staffId") Long staffId);

    /** Đếm số phiên sạc theo trạng thái cụ thể */
    long countByStatus(ChargingSessionStatus status);

    /** Lấy 5 phiên sạc mới nhất theo thời gian bắt đầu (startTime) */
    List<ChargingSession> findTop5ByOrderByStartTimeDesc();

    /** Tìm danh sách phiên sạc bắt đầu trong khoảng thời gian nhất định */
    List<ChargingSession> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /** Kiểm tra có tồn tại phiên sạc hợp lệ (PENDING, IN_PROGRESS, COMPLETED) cho bookingId không */
    @Query("""
        select case when count(cs) > 0 then true else false end
        from ChargingSession cs
        where cs.booking.bookingId = :bookingId
          and cs.status in ('PENDING','IN_PROGRESS','COMPLETED')
        """)
    Boolean existsValidSessionForBooking(Long bookingId);
}
