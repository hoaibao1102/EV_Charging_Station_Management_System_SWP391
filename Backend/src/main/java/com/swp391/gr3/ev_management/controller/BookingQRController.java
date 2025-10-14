package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.DTO.request.BookingRequest;
import com.swp391.gr3.ev_management.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingQRController {
    @Autowired
    private final BookingService bookingService;

    public BookingQRController(BookingService bookingService) { this.bookingService = bookingService; }

    // a) Lấy chuỗi payload (Base64 của JSON)
    @GetMapping("/{id}/qr-string")
    public String qrString(@PathVariable Long id) {
        return bookingService.buildQrPayload(id);
    }

    // b) Lấy ảnh QR (PNG)
    @GetMapping(value="/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] qrImage(@PathVariable Long id,
                                        @RequestParam(defaultValue = "320") int size) {
        String payload = bookingService.buildQrPayload(id);
        return bookingService.generateQrPng(payload, size);
    }

    // c) Decode thử
    @PostMapping("/qr/decode")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public BookingRequest decode(@RequestBody String base64) {
        return bookingService.decodePayload(base64.trim());
    }
}
