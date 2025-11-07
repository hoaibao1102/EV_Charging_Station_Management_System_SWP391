package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.BookingRequest;
import com.swp391.gr3.ev_management.dto.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController // Đánh dấu đây là REST controller (trả về JSON, hình ảnh, dữ liệu...)
@RequestMapping("/api/bookings") // Prefix chung cho tất cả endpoint của controller này
@RequiredArgsConstructor // Lombok: tự sinh constructor cho field final (dependency injection)
@Tag(name = "Bookings", description = "APIs for managing bookings") // Swagger: mô tả nhóm API "Bookings"
public class BookingController {

    private final BookingService bookingService; // Inject BookingService để xử lý nghiệp vụ đặt chỗ (booking)

    // ====================== CONFIRM BOOKING (XÁC NHẬN ĐẶT CHỖ) ====================== //
    @PutMapping(value = "/{bookingId}/confirm", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Confirm a booking and generate QR code", description = "Endpoint to confirm a booking and generate its QR code")
    public ResponseEntity<byte[]> confirmBooking(@PathVariable Long bookingId) {
        try {
            // ✅ 1. Xác nhận booking trong hệ thống (cập nhật trạng thái "CONFIRMED" chẳng hạn)
            bookingService.confirmBooking(bookingId);

            // ✅ 2. Xây dựng chuỗi dữ liệu (payload) cho QR code (ví dụ: chứa bookingId, user, station, ...)
            String payload = bookingService.buildQrPayload(bookingId);

            // ✅ 3. Sinh ảnh QR code PNG từ payload
            byte[] qrImage = bookingService.generateQrPng(payload, 320); // 320px là kích thước ảnh

            // ✅ 4. Trả về hình ảnh QR code (dạng byte[]) với HTTP 200
            return ResponseEntity.ok(qrImage);
        } catch (RuntimeException e) {
            // ❌ Nếu có lỗi (VD: booking không tồn tại, trạng thái không hợp lệ, ...), trả về HTTP 400
            return ResponseEntity.badRequest().build();
        }
    }

    // ====================== CANCEL BOOKING (HUỶ ĐẶT CHỖ) ====================== //
    @PutMapping("/{id}/cancel") // PUT: /api/bookings/{id}/cancel
    @Operation(summary = "Cancel Booking", description = "Cancel a pending or confirmed booking")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("id") Long bookingId) {
        // Gọi service để huỷ booking theo ID (chỉ huỷ nếu trạng thái cho phép)
        // Trả về đối tượng BookingResponse (chứa thông tin sau khi huỷ)
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    // ====================== CREATE BOOKING (TẠO MỚI ĐẶT CHỖ) ====================== //
    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new booking", description = "Endpoint to create a new booking")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        try {
            // @Valid: kiểm tra dữ liệu đầu vào (theo annotation trong DTO)
            // @RequestBody: map JSON từ client sang đối tượng CreateBookingRequest

            // ✅ Gọi service để tạo booking mới
            BookingResponse response = bookingService.createBooking(request);

            // ✅ Trả về kết quả với HTTP 201 CREATED
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // ❌ Nếu có lỗi (ví dụ: station full, thời gian trùng, dữ liệu sai, ...), trả về HTTP 400
            return ResponseEntity.badRequest().build();
        }
    }


    // ====================== DECODE QR CODE (CHỈ ADMIN/STAFF) ====================== //
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')") // Chỉ ADMIN hoặc STAFF mới được quyền giải mã QR
    @PostMapping("/qr/decode") // POST: /api/bookings/qr/decode
    @Operation(summary = "Decode booking from QR code payload", description = "Endpoint to decode booking information from a base64-encoded QR code payload")
    public BookingRequest decode(@RequestBody String base64) {
        // ✅ Giải mã payload từ QR code (base64 string)
        // bookingService.decodePayload() sẽ phân tích chuỗi base64 để lấy thông tin booking
        return bookingService.decodePayload(base64.trim()); // .trim() để loại bỏ ký tự thừa
    }

    // ====================== GET BOOKING BY ID (LẤY THÔNG TIN BOOKING) ====================== //
    @GetMapping("/{bookingId}") // GET: /api/bookings/{bookingId}
    @Operation(summary = "Get booking by ID", description = "Endpoint to retrieve booking details by booking ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long bookingId) {
        // ✅ Gọi service để lấy thông tin booking theo ID
        BookingResponse response = bookingService.getBookingById(bookingId);

        // ❌ Nếu không tìm thấy booking (null), trả về HTTP 404
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        // ✅ Nếu tìm thấy, trả về HTTP 200 cùng dữ liệu booking
        return ResponseEntity.ok(response);
    }
}
