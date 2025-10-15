package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.BookingSlot;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {
}
