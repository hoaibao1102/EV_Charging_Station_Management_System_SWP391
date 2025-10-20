package com.swp391.gr3.ev_management.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    BOOKED,
    EXPIRED,
    CANCELED,
    COMPLETED;

    @JsonValue
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
