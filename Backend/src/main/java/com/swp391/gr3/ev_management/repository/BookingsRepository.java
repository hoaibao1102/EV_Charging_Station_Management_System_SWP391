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
    // Tìm booking theo ID và status (validate trước khi start session)
    Optional<Booking> findByBookingIdAndStatus(Long bookingId, BookingStatus status);

    List<Booking> findTop50ByStatusAndScheduledEndTimeLessThanEqualOrderByScheduledEndTimeAsc(
            BookingStatus status, LocalDateTime beforeOrEqual
    );

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

    // tiện debug:
    /** Lấy status hiện tại để log/debug khi rows=0 */
    @Query("select b.status from Booking b where b.bookingId = :id")
    Optional<BookingStatus> findStatusOnly(@Param("id") Long id);
}
