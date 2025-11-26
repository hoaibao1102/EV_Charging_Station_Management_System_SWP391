package com.swp391.gr3.ev_management.controller;

import com.swp391.gr3.ev_management.dto.request.BookingRequest;
import com.swp391.gr3.ev_management.dto.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.dto.response.ConfirmedBookingView;
import com.swp391.gr3.ev_management.dto.response.ErrorResponse;
import com.swp391.gr3.ev_management.service.BookingService;
import com.swp391.gr3.ev_management.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Đánh dấu đây là REST controller (trả về JSON, hình ảnh, dữ liệu...)
@RequestMapping("/api/bookings") // Prefix chung cho tất cả endpoint của controller này
@RequiredArgsConstructor // Lombok: tự sinh constructor cho field final (dependency injection)
@Tag(name = "Bookings", description = "APIs for managing bookings") // Swagger: mô tả nhóm API "Bookings"
@Slf4j
public class BookingController {

    private final BookingService bookingService; // Inject BookingService để xử lý nghiệp vụ đặt chỗ (booking)
    private final TokenService tokenService;

    // ====================== CONFIRM BOOKING (XÁC NHẬN ĐẶT CHỖ) ====================== //
    @PutMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a booking and generate QR code",
            description = "Endpoint to confirm a booking and generate its QR code")
    public ResponseEntity<?> confirmBooking(@PathVariable Long bookingId) {
        try {
            bookingService.confirmBooking(bookingId);

            String payload = bookingService.buildQrPayload(bookingId);
            byte[] qrImage = bookingService.generateQrPng(payload, 320);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrImage);                  // ✅ success: image/png
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(e.getMessage()));   // ✅ error: application/json
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
            log.error("Create booking error", e); // thêm dòng này
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

    // ====================== GET CONFIRMED BOOKINGS FOR STAFF (LẤY BOOKING ĐÃ XÁC NHẬN THEO NHÂN VIÊN) ====================== //
    @GetMapping("/confirmed/compact")
    @Operation(summary = "Get confirmed bookings by staff",
            description = "Get all CONFIRMED bookings in stations where current staff is assigned")
    public ResponseEntity<List<ConfirmedBookingView>> getConfirmedBookingsByStaff(HttpServletRequest request) {
        // 🟢 Lấy userId từ token đăng nhập
        Long userId = tokenService.extractUserIdFromRequest(request);
        // 🟢 Gọi service
        List<ConfirmedBookingView> list = bookingService.getConfirmedBookingsForStaff(userId);

        return ResponseEntity.ok(list);
    }
}
