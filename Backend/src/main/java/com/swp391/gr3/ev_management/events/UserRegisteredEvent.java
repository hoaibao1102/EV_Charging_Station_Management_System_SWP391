package com.swp391.gr3.ev_management.events;

public record UserRegisteredEvent(Long userId, String email, String fullName) {}
