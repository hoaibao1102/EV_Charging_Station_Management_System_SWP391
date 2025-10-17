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

    // Tìm invoice theo status
    List<Invoice> findByStatus(String status);

    // Tìm invoice chưa thanh toán theo trạm
    @Query("SELECT i FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.status = 'unpaid' " +
            "ORDER BY i.issuedAt DESC")
    List<Invoice> findUnpaidInvoicesByStation(@Param("stationId") Long stationId);

    // Tìm invoice đã thanh toán theo trạm
    @Query("SELECT i FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.status = 'paid' " +
            "ORDER BY i.paidAt DESC")
    List<Invoice> findPaidInvoicesByStation(@Param("stationId") Long stationId);

    // Tìm invoice trong khoảng thời gian
    @Query("SELECT i FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.issuedAt >= :startDate " +
            "AND i.issuedAt <= :endDate " +
            "ORDER BY i.issuedAt DESC")
    List<Invoice> findByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tổng doanh thu đã thanh toán
    @Query("SELECT SUM(i.amount) FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.status = 'paid' " +
            "AND i.paidAt >= :startDate " +
            "AND i.paidAt <= :endDate")
    Double getTotalPaidAmountByStation(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm invoice chưa thanh toán
    Long countBySession_Booking_Station_StationIdAndStatus(Long stationId, String status);
}