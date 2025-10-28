package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.BookingRequest;
import com.swp391.gr3.ev_management.DTO.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "APIs for managing bookings")
public class BookingController {
    private final BookingService bookingService;

    @PutMapping(value = "/{bookingId}/confirm", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Confirm a booking and generate QR code", description = "Endpoint to confirm a booking and generate its QR code")
    public ResponseEntity<byte[]> confirmBooking(@PathVariable Long bookingId) {
        try {
            bookingService.confirmBooking(bookingId);

            // 🟢 Gọi luôn logic sinh QR
            String payload = bookingService.buildQrPayload(bookingId);
            byte[] qrImage = bookingService.generateQrPng(payload, 320);

            return ResponseEntity.ok(qrImage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel Booking", description = "Cancel a pending or confirmed booking")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("id") Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new booking", description = "Endpoint to create a new booking")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
        try {
            BookingResponse response = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }



    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PostMapping("/qr/decode")
    @Operation(summary = "Decode booking from QR code payload", description = "Endpoint to decode booking information from a base64-encoded QR code payload")
    public BookingRequest decode(@RequestBody String base64) {
        return bookingService.decodePayload(base64.trim());
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID", description = "Endpoint to retrieve booking details by booking ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.getBookingById(bookingId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
