package com.swp391.gr3.ev_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.swp391.gr3.ev_management.dto.request.BookingRequest;
import com.swp391.gr3.ev_management.dto.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.dto.response.BookingResponse;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.BookingResponseMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


@Service // Đánh dấu đây là Spring Service thực thi logic nghiệp vụ cho Booking
@RequiredArgsConstructor // Tự động generate constructor cho các field final (DI qua constructor)
@Slf4j // Cung cấp logger (log.info/error/...)
public class BookingServiceImpl implements BookingService {

    // Múi giờ cho tenant (VN)
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ==== Các dependency chính ====
    private final BookingsRepository bookingsRepository;                 // CRUD Booking
    private final BookingSlotRepository bookingSlotRepository;           // CRUD BookingSlot (mapping booking <-> slot)
    private final SlotAvailabilityRepository slotAvailabilityRepository; // CRUD SlotAvailability (tình trạng slot)
    private final UserVehicleRepository vehicleRepository;               // CRUD UserVehicle (xe người dùng)
    private final TariffRepository tariffRepository;                     // Lấy biểu giá theo loại connector
    private final NotificationsRepository notificationsRepository;       // Lưu Notification
    private final org.springframework.context.ApplicationEventPublisher eventPublisher; // Publish event (ví dụ gửi email)
    private final ObjectMapper mapper;                                   // Serialize/deserialize JSON
    private final BookingSlotLogRepository bookingSlotLogRepository;     // Lưu log các slot đã confirm
    private final BookingResponseMapper bookingResponseMapper;           // Map entity -> DTO response

    @Override
    @Transactional // Gộp tất cả bước tạo booking vào một transaction để đảm bảo toàn vẹn
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1️⃣ Lấy thông tin xe theo vehicleId. Nếu không có -> ném lỗi
        UserVehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ErrorException("Vehicle not found"));

        // 2️⃣ Lấy danh sách slot theo danh sách slotIds trong request
        List<SlotAvailability> slots = slotAvailabilityRepository.findAllById(request.getSlotIds());
        if (slots.isEmpty()) {
            throw new ErrorException("No slots found");
        }

        // Kiểm tra tất cả slot đều đang AVAILABLE (có thể đặt). Nếu có slot không AVAILABLE -> ném lỗi
        for (SlotAvailability slot : slots) {
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new ErrorException("Slot " + slot.getSlotId() + " is not available for booking");
            }
        }

        // (Giả định) Tất cả slot cùng một trạm (station), lấy từ slot đầu tiên
        ChargingStation station = slots.get(0).getTemplate().getConfig().getStation();

        // 3️⃣ Tạo thực thể Booking với thời gian dự kiến (start từ slot đầu, end từ slot cuối), trạng thái PENDING
        Booking booking = Booking.builder()
                .vehicle(vehicle)
                .station(station)
                .bookingTime(LocalDateTime.now()) // thời điểm tạo booking
                .scheduledStartTime(slots.get(0).getDate().with(slots.get(0).getTemplate().getStartTime()))
                .scheduledEndTime(slots.get(slots.size() - 1).getDate().with(slots.get(slots.size() - 1).getTemplate().getEndTime()))
                .status(BookingStatus.PENDING)
                .build();
        bookingsRepository.save(booking); // lưu booking

        // 4️⃣ Tạo BookingSlot mapping từng slot vào booking + chuyển trạng thái slot sang BOOKED (tạm giữ)
        for (SlotAvailability slot : slots) {
            BookingSlot bookingSlot = new BookingSlot();
            bookingSlot.setBooking(booking);
            bookingSlot.setSlot(slot);
            bookingSlot.setCreatedAt(LocalDateTime.now());
            bookingSlotRepository.save(bookingSlot);

            // cập nhật trạng thái slot sang BOOKED để không ai khác đặt
            slot.setStatus(SlotStatus.BOOKED);
            slotAvailabilityRepository.save(slot);
        }

        // 5️⃣ Lấy giá tham chiếu (ví dụ theo slot đầu tiên) từ biểu giá theo loại connector
        double price = slots.stream()
                .findFirst() // lấy slot đầu tiên
                .flatMap(slot -> tariffRepository.findByConnectorType(
                        slot.getChargingPoint().getConnectorType()
                ).map(Tariff::getPricePerKWh))
                .orElse(0.0);

        // 6️⃣ (Tùy chọn) Build chuỗi khung giờ để hiển thị; ở đây mapper sẽ lo format response
        String timeRanges = slots.stream()
                .map(slot -> formatTimeRange(slot.getTemplate().getStartTime(), slot.getTemplate().getEndTime()))
                .collect(Collectors.joining(", "));

        // Trả về DTO cho client (mapper tự gom dữ liệu)
        return bookingResponseMapper.forCreate(booking, slots, price);
    }

    @Override
    @Transactional // Xác nhận booking: đổi trạng thái + cố định các slot + tạo thông báo
    public BookingResponse confirmBooking(Long bookingId) {
        // Lấy booking, nếu không có -> lỗi
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        // Chỉ cho confirm nếu đang ở trạng thái PENDING
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ErrorException("Only pending bookings can be confirmed");
        }

        // Cập nhật trạng thái -> CONFIRMED + timestamp cập nhật
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now(TENANT_ZONE));
        bookingsRepository.save(booking);

        // Đảm bảo tất cả slot thuộc booking ở trạng thái BOOKED (giữ chỗ)
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability s = bs.getSlot();
            s.setStatus(SlotStatus.BOOKED);
            slotAvailabilityRepository.save(s);
        }

        // Phòng vệ: booking không có slot -> lỗi
        if (booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("No slots found for this booking");
        }

        // Lấy slot đầu tiên (để lấy stationName/connectorType/timeRange và tham chiếu giá)
        SlotAvailability slot = booking.getBookingSlots().get(0).getSlot();
        double price = tariffRepository.findByConnectorType(slot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        String stationName = slot.getTemplate().getConfig().getStation().getStationName();
        String timeRange = formatTimeRange(
                slot.getTemplate().getStartTime(),
                slot.getTemplate().getEndTime()
        );

        // Tạo Notification xác nhận booking cho user
        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setTitle("Xác nhận đặt chỗ #" + booking.getBookingId());
        noti.setContentNoti("Trạm: " + stationName + " | Khung giờ: " + timeRange
                + " | Cổng: " + slot.getChargingPoint().getConnectorType().getDisplayName());
        noti.setType(NotificationTypes.BOOKING_CONFIRMED);
        noti.setStatus(Notification.STATUS_UNREAD);
        noti.setBooking(booking);
        notificationsRepository.save(noti);

        // Xóa log cũ (nếu có) để tránh trùng lặp khi confirm lại
        if (bookingSlotLogRepository.existsByBooking_BookingId(bookingId)) {
            bookingSlotLogRepository.deleteByBooking_BookingId(bookingId);
        }

        // Sắp thứ tự các slot theo thời điểm bắt đầu để tính duration từng slot
        List<BookingSlot> ordered = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot(); var sb = b.getSlot();
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB);
                })
                .toList();

        // Ghi log duration từng slot (phục vụ thanh toán/thống kê)
        for (int i = 0; i < ordered.size(); i++) {
            BookingSlot bs = ordered.get(i);
            SlotAvailability s = bs.getSlot();
            var start = s.getDate().with(s.getTemplate().getStartTime());
            var end   = s.getDate().with(s.getTemplate().getEndTime());
            int durationMin = (int) java.time.Duration.between(start, end).toMinutes();

            BookingSlotLog logEntry = BookingSlotLog.builder()
                    .booking(booking)
                    .slotIndex(i)             // vị trí slot trong chuỗi
                    .slotDurationMin(durationMin) // số phút của slot
                    .build();
            bookingSlotLogRepository.save(logEntry);
        }

        // Publish event để các listener khác xử lý (ví dụ: email/push)
        eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

        // Trả về DTO sau khi confirm
        return bookingResponseMapper.forConfirm(booking, slot, price, timeRange);
    }

    // Hàm format "HH:mm - HH:mm" để hiển thị khung giờ.
    // Lưu ý: tham số hiện đang là LocalDateTime theo chữ ký, nhưng thực tế truyền LocalTime từ template.
    // Code gốc dùng như vậy, nên giữ nguyên chữ ký hàm và giả định đã chuyển đổi phù hợp ở nơi gọi/overload khác.
    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        var f = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return start.format(f) + " - " + end.format(f);
    }

    /** 1) Build payload QR: gom dữ liệu Booking thành DTO -> JSON -> Base64 (URL-safe) */
    @Override
    public String buildQrPayload(Long bookingId) {
        // Lấy booking theo id, không có thì ném lỗi
        Booking b = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + bookingId));

        // Map entity -> DTO tối giản để nhúng vào QR
        BookingRequest dto = new BookingRequest();
        dto.setBookingId(b.getBookingId());
        dto.setStationId(b.getStation().getStationId()); // lưu ID thay vì object
        dto.setVehicleId(b.getVehicle().getVehicleId());
        dto.setBookingTime(b.getBookingTime());
        dto.setScheduledStartTime(b.getScheduledStartTime());
        dto.setScheduledEndTime(b.getScheduledEndTime());
        dto.setStatus(String.valueOf(b.getStatus()));

        // Serialize DTO -> JSON -> Base64 URL-safe (không padding) để gọn & an toàn khi nhúng QR
        try {
            String json = mapper.writeValueAsString(dto);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ErrorException("Failed to serialize QR payload");
        }
    }

    /** 2) Sinh ảnh QR (PNG) từ chuỗi payload đã mã hóa */
    @Override
    public byte[] generateQrPng(String payload, int size) {
        try {
            // Tạo ma trận QR từ payload
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);

            // Ghi ảnh PNG vào byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ErrorException("Failed to generate QR image");
        }
    }

    /** 3) Giải mã payload từ Base64 URL-safe -> JSON -> DTO */
    @Override
    public BookingRequest decodePayload(String base64) {
        try {
            String json = new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
            return mapper.readValue(json, BookingRequest.class);
        } catch (Exception e) {
            throw new ErrorException("Failed to decode QR payload");
        }
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        // Lấy booking theo id; không có -> lỗi
        Booking b = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));
        // Map sang DTO view (đầy đủ thông tin cần thiết để hiển thị)
        return bookingResponseMapper.view(b);
    }

    @Override
    @Transactional // Hủy booking: cập nhật trạng thái + trả slot + thông báo
    public BookingResponse cancelBooking(Long bookingId) {
        // 1) Tìm booking; không có -> lỗi
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        // 2) Ràng buộc trạng thái (không hủy nếu đã CANCELED/COMPLETED)
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new ErrorException("Booking đã bị hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ErrorException("Không thể hủy booking đã hoàn thành");
        }

        // 3) Đảm bảo có slot để tính thời điểm bắt đầu (phục vụ rule hủy trước 30 phút)
        if (booking.getBookingSlots() == null || booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("Booking không có slot nào để xác định thời gian bắt đầu");
        }

        // Lấy slot sớm nhất trong booking (so sánh theo startTime thực tế)
        SlotAvailability firstSlot = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot();
                    var sb = b.getSlot();
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB);
                })
                .findFirst()
                .get()
                .getSlot();

        // Tính thời điểm bắt đầu và thời điểm hiện tại
        LocalDateTime slotStart = firstSlot.getDate().with(firstSlot.getTemplate().getStartTime());
        LocalDateTime now = LocalDateTime.now();

        // 4) Rule: Không cho hủy nếu còn < 30 phút trước khi bắt đầu slot đầu tiên
        if (!now.isBefore(slotStart.minusMinutes(30))) {
            throw new ErrorException("Không thể hủy đặt chỗ khi còn dưới 30 phút trước thời gian bắt đầu.");
        }

        // 5) Cập nhật trạng thái booking -> CANCELED
        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingsRepository.save(booking);

        // 6) Giải phóng tất cả slot của booking về AVAILABLE
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability slot = bs.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            slotAvailabilityRepository.save(slot);
        }

        // 7) Gửi Notification cho user (nếu lỗi thông báo thì chỉ log warning, không rollback)
        try {
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser());
            noti.setTitle("Hủy đặt chỗ #" + booking.getBookingId());
            noti.setContentNoti("Đặt chỗ của bạn tại trạm "
                    + booking.getStation().getStationName()
                    + " đã được hủy thành công.");
            noti.setType(NotificationTypes.BOOKING_CANCELED);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setBooking(booking);
            notificationsRepository.save(noti);

            // Publish event để trigger các kênh thông báo khác (email/push)
            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        } catch (Exception e) {
            log.warn("[cancelBooking] Notification failed: {}", e.getMessage());
        }

        // 8) Lấy giá tham chiếu theo connector của slot đầu tiên (phục vụ hiển thị)
        double price = tariffRepository.findByConnectorType(firstSlot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        // Trả về DTO hủy booking
        return bookingResponseMapper.forCancel(booking, booking.getBookingSlots(), firstSlot, price);
    }
}
