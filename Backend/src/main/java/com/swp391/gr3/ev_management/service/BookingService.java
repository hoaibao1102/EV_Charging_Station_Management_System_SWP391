package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.BookingRequest;
import com.swp391.gr3.ev_management.DTO.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import org.springframework.stereotype.Service;

@Service
public interface BookingService {
    // Tạo booking mới (PENDING)
    BookingResponse createBooking(CreateBookingRequest request);

    // Xác nhận booking (CONFIRMED)
    BookingResponse confirmBooking(Long bookingId);

    String buildQrPayload(Long bookingId);
    byte[] generateQrPng(String payload, int size);
    BookingRequest decodePayload(String base64);
}
