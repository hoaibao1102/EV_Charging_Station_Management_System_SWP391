package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    public List<Booking> findByStartTimeBetween(LocalDateTime scheduledStartTime, LocalDateTime scheduledEndTime);
}
