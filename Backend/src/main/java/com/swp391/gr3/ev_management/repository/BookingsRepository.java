package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.dto.request.LightBookingInfo;
import com.swp391.gr3.ev_management.dto.response.ConfirmedBookingView;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingsRepository extends JpaRepository<Booking,Long> {
    // ✅ Tìm một Booking theo bookingId và status cụ thể.
    //    Thường dùng để "validate trạng thái" trước các thao tác nhạy cảm (vd: start session chỉ khi status=CONFIRMED).
    Optional<Booking> findByBookingIdAndStatus(Long bookingId, BookingStatus status);

    // ✅ Lấy tối đa 50 booking có status cho trước và có scheduledEndTime <= thời điểm truyền vào,
    //    sắp xếp tăng dần theo scheduledEndTime (dùng cho job thu dọn/auto-cancel/auto-complete theo hạn).
    List<Booking> findTop50ByStatusInAndScheduledEndTimeLessThanEqualOrderByScheduledEndTimeAsc(
            List<BookingStatus> statuses,
            LocalDateTime now
    );

    // ✅ Lấy một booking kèm TẤT CẢ các quan hệ cần thiết để hiển thị/ xử lý:
    //    - vehicle -> driver -> user
    //    - bookingSlots -> slot -> chargingPoint -> connectorType
    //    - station
    //    join fetch để tránh N+1 và LazyInitializationException.
    @Query("""
      SELECT DISTINCT b FROM Booking b
      JOIN FETCH b.vehicle v
      JOIN FETCH v.driver d
      JOIN FETCH d.user u
      LEFT JOIN FETCH b.bookingSlots bs
      LEFT JOIN FETCH bs.slot s
      LEFT JOIN FETCH s.chargingPoint cp
      LEFT JOIN FETCH cp.connectorType ct
      LEFT JOIN FETCH b.station st
      WHERE b.bookingId = :bookingId
    """)
    Optional<Booking> findByIdWithAllNeeded(@Param("bookingId") Long bookingId);

    // ✅ Lấy một booking chủ yếu để xác định "connector type" đang dùng:
    //    - fetch bookingSlots -> slot -> chargingPoint -> connectorType (của point)
    //    - fetch vehicle -> model -> connectorType (của xe)
    //    Dùng khi cần so khớp/kiểm tra tương thích loại đầu nối giữa xe và điểm sạc.
    @Query("""
  SELECT DISTINCT b FROM Booking b
  LEFT JOIN FETCH b.bookingSlots bs
  LEFT JOIN FETCH bs.slot s
  LEFT JOIN FETCH s.chargingPoint cp
  LEFT JOIN FETCH cp.connectorType ct
  LEFT JOIN FETCH b.vehicle v
  LEFT JOIN FETCH v.model vm
  LEFT JOIN FETCH vm.connectorType vct
  WHERE b.bookingId = :bookingId
""")
    Optional<Booking> findByIdWithConnectorType(@Param("bookingId") Long bookingId);

    // ✅ Cập nhật trạng thái của một booking nếu trạng thái hiện tại nằm trong danh sách cho phép.
    //    Trả về số row bị ảnh hưởng (0 hoặc 1).
    @Modifying
    @Query("""
    UPDATE Booking b 
    SET b.status = :newStatus, b.updatedAt = :now 
    WHERE b.bookingId = :bookingId 
      AND b.status IN (:allowedStatuses)
""")
    int updateStatusIfIn(
            @Param("bookingId") Long bookingId,
            @Param("newStatus") BookingStatus newStatus,
            @Param("allowedStatuses") List<BookingStatus> allowedStatuses,
            @Param("now") LocalDateTime now
    );

    // ✅ Tiện debug/log: chỉ lấy status hiện tại của một booking theo id.
    //    Hữu ích khi updateStatusIfMatches trả về 0 rows -> kiểm tra trạng thái thật sự đang là gì.
    /** Lấy status hiện tại để log/debug khi rows=0 */
    @Query("select b.status from Booking b where b.bookingId = :id")
    Optional<BookingStatus> findStatusOnly(@Param("id") Long id);

    /**
     * ✅ Lấy danh sách Booking có trạng thái CONFIRMED,
     *    nhưng CHỈ ở các trạm mà staff (staffId) hiện đang được assign.
     *
     * - DISTINCT: tránh trùng do booking có nhiều bookingSlot.
     * - EXISTS: kiểm tra staff có assignment active ở trạm đó:
     *      + ss.station = st
     *      + ss.staff.staffId = :staffId
     *      + ss.assignedAt <= CURRENT_TIMESTAMP
     *      + (ss.unassignedAt IS NULL OR ss.unassignedAt > CURRENT_TIMESTAMP)
     */
    @Query("""
    select distinct new com.swp391.gr3.ev_management.dto.response.ConfirmedBookingView(
        ct.displayName,
        cp.pointNumber,
        st.stationName,
        u.name,
        v.vehiclePlate,
        b.scheduledStartTime,
        b.scheduledEndTime
    )
    from Booking b
        join b.vehicle v
        join v.model vm
        join vm.connectorType ct
        join b.bookingSlots bs
        join bs.slot sl
        join sl.chargingPoint cp
        join cp.station st
        join v.driver d
        join d.user u
    where b.status = com.swp391.gr3.ev_management.enums.BookingStatus.CONFIRMED
      and exists (
            select 1
            from StationStaff ss
            where ss.station = st
              and ss.staff.staffId = :staffId
              and ss.assignedAt <= CURRENT_TIMESTAMP
              and (ss.unassignedAt is null or ss.unassignedAt > CURRENT_TIMESTAMP)
      )
    order by b.scheduledStartTime asc
""")
    List<ConfirmedBookingView> findConfirmedBookingsByStaff(@Param("staffId") Long staffId);

    /** Đếm số booking được tạo trong khoảng thời gian nhất định */
    long countByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    /** Tìm danh sách booking theo trạng thái và khoảng thời gian bắt đầu */
    List<Booking> findByStatusAndScheduledStartTimeBetween(
            BookingStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    /** Lấy 5 booking mới nhất theo thời gian tạo */
    List<Booking> findTop5ByOrderByCreatedAtDesc();


    /** Lấy thông tin booking nhẹ để gửi email */
    @Query("""
        select new com.swp391.gr3.ev_management.dto.request.LightBookingInfo(
            b.bookingId,
            b.scheduledStartTime,
            b.scheduledEndTime,
            v.vehicleId
        )
        from Booking b
        join b.vehicle v
        where b.bookingId = :bookingId
    """)
    Optional<LightBookingInfo> findLightBookingInfo(Long bookingId);

    /** Lấy tối đa 50 ID booking quá hạn theo list trạng thái */
    @Query("""
        select b.bookingId
        from Booking b
        where b.status in :statuses
          and b.scheduledEndTime <= :now
        order by b.scheduledEndTime asc
    """)
    List<Long> findOverdueIds(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("now") LocalDateTime now
    );

    /** Lấy thông tin tối thiểu để dùng cho Violation + Notification khi overdue */
    @Query("""
        select u.userId as userId,
               st.stationName as stationName,
               b.scheduledEndTime as scheduledEndTime
        from Booking b
            join b.vehicle v
            join v.driver d
            join d.user u
            join b.station st
        where b.bookingId = :bookingId
    """)
    java.util.Optional<com.swp391.gr3.ev_management.repository.BookingOverdueView>
    findOverdueView(@Param("bookingId") Long bookingId);
}
