package com.swp391.gr3.ev_management.repository;

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
    List<Booking> findTop50ByStatusAndScheduledEndTimeLessThanEqualOrderByScheduledEndTimeAsc(
            BookingStatus status, LocalDateTime beforeOrEqual
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

    // ✅ Cập nhật trạng thái booking theo điều kiện "so khớp trạng thái hiện tại":
    //    - Chỉ update khi b.status đang bằng fromStatus (optimistic conditional update để tránh race condition).
    //    - Đồng thời cập nhật updatedAt.
    //    - @Modifying + clear/flush auto để đồng bộ persistence context.
    //    Trả về số dòng cập nhật (0 nếu không khớp fromStatus, 1 nếu thành công).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Booking b
           set b.status    = :toStatus,
               b.updatedAt = :updatedAt
         where b.bookingId = :id
           and b.status    = :fromStatus
    """)
    int updateStatusIfMatches(@Param("id") Long id,
                              @Param("fromStatus") BookingStatus fromStatus,
                              @Param("toStatus") BookingStatus toStatus,
                              @Param("updatedAt") LocalDateTime updatedAt);

    // ✅ Tiện debug/log: chỉ lấy status hiện tại của một booking theo id.
    //    Hữu ích khi updateStatusIfMatches trả về 0 rows -> kiểm tra trạng thái thật sự đang là gì.
    /** Lấy status hiện tại để log/debug khi rows=0 */
    @Query("select b.status from Booking b where b.bookingId = :id")
    Optional<BookingStatus> findStatusOnly(@Param("id") Long id);
}
