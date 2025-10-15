package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.BookingRequest;
import com.swp391.gr3.ev_management.service.BookingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingQRController {
    @Autowired
    private final BookingsService bookingsService;

    public BookingQRController(BookingsService bookingsService) { this.bookingsService = bookingsService; }

    // a) Lấy chuỗi payload (Base64 của JSON)
    @GetMapping("/{id}/qr-string")
    public String qrString(@PathVariable Long id) {
        return bookingsService.buildQrPayload(id);
    }

    // b) Lấy ảnh QR (PNG)
    @GetMapping(value="/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] qrImage(@PathVariable Long id,
                                        @RequestParam(defaultValue = "320") int size) {
        String payload = bookingsService.buildQrPayload(id);
        return bookingsService.generateQrPng(payload, size);
    }

    // c) Decode thử
    @PostMapping("/qr/decode")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public BookingRequest decode(@RequestBody String base64) {
        return bookingsService.decodePayload(base64.trim());
    }
}
