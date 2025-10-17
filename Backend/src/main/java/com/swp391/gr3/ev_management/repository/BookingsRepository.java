package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingsRepository extends JpaRepository<Booking,Long> {
    // Tìm booking theo ID và status (validate trước khi start session)
    Optional<Booking> findByBookingIdAndStatus(Long bookingId, BookingStatus status);

}
