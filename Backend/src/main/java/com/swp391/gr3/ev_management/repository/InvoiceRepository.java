package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}