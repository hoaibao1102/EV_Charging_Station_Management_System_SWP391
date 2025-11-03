package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice,Long> {
    // Tìm invoice theo session ID (quan trọng để confirm payment)
    Optional<Invoice> findBySession_SessionId(Long sessionId);

    // Tìm invoice chưa thanh toán theo trạm
    @Query("SELECT i FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.status = 'unpaid' " +
            "ORDER BY i.issuedAt DESC")
    List<Invoice> findUnpaidInvoicesByStation(@Param("stationId") Long stationId);

    @Query("""
      SELECT COALESCE(SUM(i.amount), 0)
      FROM Invoice i
      WHERE i.issuedAt BETWEEN :from AND :to
    """)
    double sumAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Tổng doanh thu toàn hệ thống (từ đầu)
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i")
    double sumAll();

    // Doanh thu theo trạm (join qua Session->Booking->Station)
    @Query("""
      SELECT COALESCE(SUM(i.amount), 0)
      FROM Invoice i
      JOIN i.session s
      JOIN s.booking b
      WHERE b.station.stationId = :stationId
        AND i.issuedAt BETWEEN :from AND :to
    """)
    double sumByStationBetween(@Param("stationId") Long stationId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

}