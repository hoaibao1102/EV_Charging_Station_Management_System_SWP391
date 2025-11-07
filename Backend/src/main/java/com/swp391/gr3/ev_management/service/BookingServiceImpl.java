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


@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingsRepository bookingsRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final UserVehicleRepository vehicleRepository;
    private final TariffRepository tariffRepository;
    private final NotificationsRepository notificationsRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper mapper;
    private final BookingSlotLogRepository bookingSlotLogRepository;
    private final BookingOverdueHandler overdueHandler;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1️⃣ Lấy thông tin xe
        UserVehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ErrorException("Vehicle not found"));

        // 2️⃣ Lấy danh sách slot
        List<SlotAvailability> slots = slotAvailabilityRepository.findAllById(request.getSlotIds());
        if (slots.isEmpty()) {
            throw new ErrorException("No slots found");
        }

        // Kiểm tra tất cả đều AVAILABLE và cùng trạm
        for (SlotAvailability slot : slots) {
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new ErrorException("Slot " + slot.getSlotId() + " is not available for booking");
            }
        }

        // Giả sử tất cả slot cùng trạm
        ChargingStation station = slots.get(0).getTemplate().getConfig().getStation();

        // 3️⃣ Tạo Booking
        Booking booking = Booking.builder()
                .vehicle(vehicle)
                .station(station)
                .bookingTime(LocalDateTime.now())
                .scheduledStartTime(slots.get(0).getDate().with(slots.get(0).getTemplate().getStartTime()))
                .scheduledEndTime(slots.get(slots.size() - 1).getDate().with(slots.get(slots.size() - 1).getTemplate().getEndTime()))
                .status(BookingStatus.PENDING)
                .build();
        bookingsRepository.save(booking);

        // 4️⃣ Tạo BookingSlot cho từng slot
        for (SlotAvailability slot : slots) {
            BookingSlot bookingSlot = new BookingSlot();
            bookingSlot.setBooking(booking);
            bookingSlot.setSlot(slot);
            bookingSlot.setCreatedAt(LocalDateTime.now());
            bookingSlotRepository.save(bookingSlot);

            // cập nhật trạng thái slot
            slot.setStatus(SlotStatus.BOOKED);
            slotAvailabilityRepository.save(slot);
        }

        // 5️⃣ Lấy giá và tính tổng (tuỳ bạn muốn theo giờ hay cộng gộp)
        double price = slots.stream()
                .findFirst() // lấy slot đầu tiên
                .flatMap(slot -> tariffRepository.findByConnectorType(
                        slot.getChargingPoint().getConnectorType()
                ).map(Tariff::getPricePerKWh))
                .orElse(0.0);

        // 6️⃣ Build response
        String timeRanges = slots.stream()
                .map(slot -> formatTimeRange(slot.getTemplate().getStartTime(), slot.getTemplate().getEndTime()))
                .collect(Collectors.joining(", "));

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(vehicle.getModel().getModel())
                .stationName(station.getStationName())
                .slotName("Slots: " + slots.stream()
                        .map(s -> s.getSlotId().toString())
                        .collect(Collectors.joining(", ")))
                .connectorType(slots.get(0).getChargingPoint().getConnectorType().getDisplayName())
                .timeRange(timeRanges)
                .bookingDate(slots.get(0).getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ErrorException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now(TENANT_ZONE));
        bookingsRepository.save(booking);

        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability s = bs.getSlot();
            s.setStatus(SlotStatus.BOOKED);
            slotAvailabilityRepository.save(s);
        }

        if (booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("No slots found for this booking");
        }

        SlotAvailability slot = booking.getBookingSlots().get(0).getSlot();
        double price = tariffRepository.findByConnectorType(slot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        String stationName = slot.getTemplate().getConfig().getStation().getStationName();
        String timeRange = formatTimeRange(
                slot.getTemplate().getStartTime(),
                slot.getTemplate().getEndTime()
        );

        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser());
        noti.setTitle("Xác nhận đặt chỗ #" + booking.getBookingId());
        noti.setContentNoti("Trạm: " + stationName + " | Khung giờ: " + timeRange
                + " | Cổng: " + slot.getChargingPoint().getConnectorType().getDisplayName());
        noti.setType(NotificationTypes.BOOKING_CONFIRMED);
        noti.setStatus(Notification.STATUS_UNREAD);
        noti.setBooking(booking);
        notificationsRepository.save(noti);

        if (bookingSlotLogRepository.existsByBooking_BookingId(bookingId)) {
            bookingSlotLogRepository.deleteByBooking_BookingId(bookingId);
        }

        List<BookingSlot> ordered = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot(); var sb = b.getSlot();
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB);
                })
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            BookingSlot bs = ordered.get(i);
            SlotAvailability s = bs.getSlot();
            var start = s.getDate().with(s.getTemplate().getStartTime());
            var end   = s.getDate().with(s.getTemplate().getEndTime());
            int durationMin = (int) java.time.Duration.between(start, end).toMinutes();

            BookingSlotLog logEntry = BookingSlotLog.builder()
                    .booking(booking)
                    .slotIndex(i)
                    .slotDurationMin(durationMin)
                    .build();
            bookingSlotLogRepository.save(logEntry);
        }

        eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(booking.getVehicle().getModel().getModel())
                .stationName(stationName)
                .slotName("Slot " + slot.getSlotId())
                .connectorType(slot.getChargingPoint().getConnectorType().getDisplayName())
                .timeRange(timeRange)
                .bookingDate(slot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }

    // ❌ BỎ scheduler trùng ở đây

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        var f = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return start.format(f) + " - " + end.format(f);
    }

    /** 1) Lấy dữ liệu và serialize -> Base64 (chuỗi để nhúng vào QR) */
    @Override
    public String buildQrPayload(Long bookingId) {
        Booking b = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + bookingId));

        BookingRequest dto = new BookingRequest();
        dto.setBookingId(b.getBookingId());
        dto.setStationId(b.getStation().getStationId()); // map sang ID
        dto.setVehicleId(b.getVehicle().getVehicleId());
        dto.setBookingTime(b.getBookingTime());
        dto.setScheduledStartTime(b.getScheduledStartTime());
        dto.setScheduledEndTime(b.getScheduledEndTime());
        dto.setStatus(String.valueOf(b.getStatus()));

        try {
            String json = mapper.writeValueAsString(dto);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ErrorException("Failed to serialize QR payload");
        }
    }

    /** 2) Tạo ảnh QR PNG từ chuỗi payload */
    @Override
    public byte[] generateQrPng(String payload, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ErrorException("Failed to generate QR image");
        }
    }

    /** 3) Giải mã ngược khi cần */
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
        return mapper.convertValue(
                bookingsRepository.findById(bookingId)
                        .orElseThrow(() -> new ErrorException("Booking not found")),
                BookingResponse.class
        );
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new ErrorException("Booking đã bị hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ErrorException("Không thể hủy booking đã hoàn thành");
        }

        // ✅ Lấy slot sớm nhất trong booking
        if (booking.getBookingSlots() == null || booking.getBookingSlots().isEmpty()) {
            throw new ErrorException("Booking không có slot nào để xác định thời gian bắt đầu");
        }

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

        LocalDateTime slotStart = firstSlot.getDate().with(firstSlot.getTemplate().getStartTime());
        LocalDateTime now = LocalDateTime.now();

        // ✅ Không cho hủy nếu còn dưới 30 phút
        if (!now.isBefore(slotStart.minusMinutes(30))) {
            throw new ErrorException("Không thể hủy đặt chỗ khi còn dưới 30 phút trước thời gian bắt đầu.");
        }

        // ✅ Cập nhật trạng thái booking
        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingsRepository.save(booking);

        // ✅ Giải phóng lại các slot
        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability slot = bs.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            slotAvailabilityRepository.save(slot);
        }

        // ✅ Gửi Notification cho user (optional)
        try {
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser());
            noti.setTitle("Hủy đặt chỗ #" + booking.getBookingId());
            noti.setContentNoti("Đặt chỗ của bạn tại trạm "
                    + booking.getStation().getStationName()
                    + " đã được hủy thành công.");
            noti.setType(NotificationTypes.BOOKING_CANCELED);
            noti.setStatus("UNREAD");
            noti.setBooking(booking);
            notificationsRepository.save(noti);

            eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
        } catch (Exception e) {
            log.warn("[cancelBooking] Notification failed: {}", e.getMessage());
        }

        double price = tariffRepository.findByConnectorType(firstSlot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .vehicleName(booking.getVehicle().getModel().getModel())
                .stationName(booking.getStation().getStationName())
                .slotName("Slots: " + booking.getBookingSlots().stream()
                        .map(bs -> bs.getSlot().getSlotId().toString())
                        .collect(Collectors.joining(", ")))
                .connectorType(firstSlot.getChargingPoint().getConnectorType().getDisplayName())
                .bookingDate(firstSlot.getDate())
                .price(price)
                .status(booking.getStatus())
                .build();
    }
}