package com.swp391.gr3.ev_management.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SlotStatus {
    AVAILABLE, BOOKED, MAINTENANCE;

    @JsonValue
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
