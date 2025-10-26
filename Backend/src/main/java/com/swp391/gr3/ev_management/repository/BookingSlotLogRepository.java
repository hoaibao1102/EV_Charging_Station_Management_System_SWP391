package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.BookingSlotLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSlotLogRepository extends JpaRepository<BookingSlotLog, Long> {
    void deleteByBooking_BookingId(Long bookingId);
    boolean existsByBooking_BookingId(Long bookingId);
}
