package com.swp391.gr3.ev_management.repository;

import java.time.LocalDateTime;

public interface BookingOverdueView {
    Long getUserId();
    String getStationName();
    LocalDateTime getScheduledEndTime();
}
