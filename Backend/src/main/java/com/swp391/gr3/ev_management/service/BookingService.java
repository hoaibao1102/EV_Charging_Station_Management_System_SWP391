package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.entity.Booking;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BookingService {
    // Tạo booking mới (PENDING)
    BookingResponse createBooking(CreateBookingRequest request);

    // Xác nhận booking (CONFIRMED)
    BookingResponse confirmBooking(Long bookingId);
}
