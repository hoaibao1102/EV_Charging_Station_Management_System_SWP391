package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.BookingRequest;

public interface BookingsService {
    String buildQrPayload(Long bookingId);
    byte[] generateQrPng(String payload, int size);
    BookingRequest decodePayload(String base64);

}
