package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    // Tìm transaction theo trạm
    List<Transaction> findByInvoice_Session_Booking_Station_StationId(Long stationId);

    // Tìm transaction theo invoice ID
    List<Transaction> findByInvoice_InvoiceId(Long invoiceId);

    // Tìm transaction thành công theo invoice
    Optional<Transaction> findByInvoice_InvoiceIdAndStatus(Long invoiceId, String status);

    // Tìm transaction theo payment method
    List<Transaction> findByPaymentMethod_PaymentMethodId(Long paymentMethodId);

    // Tìm transaction theo status
    List<Transaction> findByStatus(String status);

    // Tìm transaction trong khoảng thời gian
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.invoice.session.booking.station.stationId = :stationId " +
            "AND t.createdAt >= :startDate " +
            "AND t.createdAt <= :endDate " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
