package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.BookingRequest;
import com.swp391.gr3.ev_management.dto.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.dto.response.ConfirmedBookingView;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface BookingService {
    // Tạo booking mới (PENDING)
    BookingResponse createBooking(CreateBookingRequest request);

    // Xác nhận booking (CONFIRMED)
    BookingResponse confirmBooking(Long bookingId);

    String buildQrPayload(Long bookingId);
    byte[] generateQrPng(String payload, int size);
    BookingRequest decodePayload(String base64);

    BookingResponse getBookingById(Long bookingId);

    BookingResponse cancelBooking(Long bookingId);

    List<ConfirmedBookingView> getConfirmedBookingsForStaff(Long userId);

    Optional<Booking> findByBookingIdAndStatus(Long bookingId, BookingStatus bookingStatus);

    void save(Booking booking);

    Optional<Booking> findByIdWithConnectorType(Long bookingId);
}
