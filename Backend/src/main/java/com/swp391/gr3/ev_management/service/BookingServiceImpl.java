package com.swp391.gr3.ev_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.swp391.gr3.ev_management.DTO.request.BookingRequest;
import com.swp391.gr3.ev_management.DTO.request.CreateBookingRequest;
import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.DTO.response.BookingResponse;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingsRepository bookingsRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final UserVehicleRepository vehicleRepository;
    private final TariffRepository tariffRepository;
    private final NotificationsRepository notificationsRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper mapper;
    private final ChargingSessionRepository chargingSessionRepository;
    private final ViolationService violationService;
    private final BookingSlotLogRepository bookingSlotLogRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1️⃣ Lấy thông tin xe
        UserVehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // 2️⃣ Lấy danh sách slot
        List<SlotAvailability> slots = slotAvailabilityRepository.findAllById(request.getSlotIds());
        if (slots.isEmpty()) {
            throw new RuntimeException("No slots found");
        }

        // Kiểm tra tất cả đều AVAILABLE và cùng trạm
        for (SlotAvailability slot : slots) {
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new RuntimeException("Slot " + slot.getSlotId() + " is not available for booking");
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
        double totalPrice = slots.stream()
                .mapToDouble(slot -> tariffRepository.findByConnectorType(slot.getChargingPoint().getConnectorType())
                        .map(Tariff::getPricePerKWh)
                        .orElse(0.0))
                .sum();

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
                .price(totalPrice)
                .status(booking.getStatus())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingsRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingsRepository.save(booking);

        for (BookingSlot bs : booking.getBookingSlots()) {
            SlotAvailability s = bs.getSlot();
            s.setStatus(SlotStatus.BOOKED);
            slotAvailabilityRepository.save(s);
        }

        if (booking.getBookingSlots().isEmpty()) {
            throw new RuntimeException("No slots found for this booking");
        }

        SlotAvailability slot = booking.getBookingSlots().get(0).getSlot();
        double price = tariffRepository.findByConnectorType(slot.getChargingPoint().getConnectorType())
                .map(Tariff::getPricePerKWh)
                .orElse(0.0);

        String stationName = slot.getTemplate().getConfig().getStation().getStationName();
        String timeRange = formatTimeRange(
                slot.getTemplate().getStartTime(),   // LocalTime
                slot.getTemplate().getEndTime()      // LocalTime
        );

        // 🔔 Tạo Notification 1 lần
        Notification noti = new Notification();
        noti.setUser(booking.getVehicle().getDriver().getUser()); // chỉnh theo model thật
        noti.setTitle("Xác nhận đặt chỗ #" + booking.getBookingId());
        noti.setContentNoti("Trạm: " + stationName + " | Khung giờ: " + timeRange
                + " | Cổng: " + slot.getChargingPoint().getConnectorType().getDisplayName());
        noti.setType(NotificationTypes.BOOKING_CONFIRMED); // enum
        noti.setStatus("UNREAD");
        noti.setBooking(booking);                          // dùng quan hệ booking
        notificationsRepository.save(noti);

        // --- Ghi log BookingSlotLogs ---
        if (bookingSlotLogRepository.existsByBooking_BookingId(bookingId)) {
            // tránh nhân bản log khi confirm lại
            bookingSlotLogRepository.deleteByBooking_BookingId(bookingId);
        }

        // Sắp xếp slot theo thời gian bắt đầu thực tế
        List<BookingSlot> ordered = booking.getBookingSlots().stream()
                .sorted((a, b) -> {
                    var sa = a.getSlot();
                    var sb = b.getSlot();
                    var startA = sa.getDate().with(sa.getTemplate().getStartTime());
                    var startB = sb.getDate().with(sb.getTemplate().getStartTime());
                    return startA.compareTo(startB);
                })
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            BookingSlot bs = ordered.get(i);
            SlotAvailability s = bs.getSlot();

            // Tính thời lượng phút của slot
            var start = s.getDate().with(s.getTemplate().getStartTime());
            var end   = s.getDate().with(s.getTemplate().getEndTime());
            int durationMin = (int) java.time.Duration.between(start, end).toMinutes();

            BookingSlotLog log = BookingSlotLog.builder()
                    .booking(booking)
                    .slotIndex(i)              // 0,1,2...
                    .slotDurationMin(durationMin)
                    .build();

            bookingSlotLogRepository.save(log);
        }

        // 📣 Publish event đúng 1 lần
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
                .status(BookingStatus.valueOf(booking.getStatus().toString()))
                .build();
    }

    /** Quét định kỳ — nếu quá giờ thì hủy booking & tạo violation */
    @Scheduled(fixedDelay = 10_000) // mỗi 10 giây
    public void autoCancelOverdueBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> overdue = bookingsRepository
                .findTop50ByStatusAndScheduledEndTimeBeforeOrderByScheduledEndTimeAsc(
                        BookingStatus.CONFIRMED, now);

        log.info("[autoCancelOverdue] found {} bookings overdue at {}", overdue.size(), now);

        for (Booking b : overdue) {
            try {
                cancelAndCreateViolationTx(b.getBookingId()); // chỉ truyền ID, không truyền entity detach
            } catch (Exception ex) {
                log.error("[autoCancelOverdue] Error bookingId={}: {}", b.getBookingId(), ex.getMessage(), ex);
            }
        }
    }

    /** Hủy booking quá hạn và tạo violation (TX riêng) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAndCreateViolationTx(Long bookingId) {
        // ⚠️ Load lại trong TX để có session + fetch đủ associations
        Booking booking = bookingsRepository.findByIdWithAllNeeded(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Idempotent: nếu đã cancel (hoặc completed) thì bỏ qua
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.debug("[autoCancelOverdue] skip bookingId={} (status={})", bookingId, booking.getStatus());
            return;
        }

        // 1) Cập nhật Booking
        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingsRepository.saveAndFlush(booking);
        log.info("[autoCancelOverdue] canceled bookingId={}", bookingId);

        // 2) Tạo Violation (không để lỗi nhỏ làm rollback TX)
        try {
            Long userId = booking.getVehicle().getDriver().getUser().getUserId();

            ViolationRequest vr = ViolationRequest.builder()
                    .bookingId(bookingId) // BẮT BUỘC
                    .description(String.format(
                            "Booking #%d đã quá hạn và bị hủy tự động (đến %s tại trạm %s).",
                            bookingId, booking.getScheduledEndTime(), booking.getStation().getStationName()))
                    .build();

            var resp = violationService.createViolation(userId, vr);
            if (resp == null) {
                log.warn("[autoCancelOverdue] violation NOT created (resp=null) for bookingId={}", bookingId);
            } else {
                log.info("[autoCancelOverdue] violation created for bookingId={}, violationId={}",
                        bookingId, resp.getViolationId());
            }
        } catch (Exception e) {
            // chỉ log, không throw để TX này vẫn commit phần booking đã cancel
            log.error("[autoCancelOverdue] createViolation failed for bookingId={}: {}", bookingId, e.getMessage(), e);
        }

        // 3) Notification — bọc riêng để không làm rollback
        try {
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser());
            noti.setTitle("Booking bị hủy do quá hạn");
            noti.setContentNoti("Booking #" + bookingId + " đã bị hủy vì quá giờ.");
            noti.setType(NotificationTypes.BOOKING_OVERDUE);
            noti.setStatus("UNREAD");
            noti.setBooking(booking);
            notificationsRepository.save(noti);

            // publish event có thể thất bại — cũng bắt lại
            try {
                eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
            } catch (Exception mailEx) {
                log.warn("[autoCancelOverdue] publish email event failed (bookingId={}): {}",
                        bookingId, mailEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("[autoCancelOverdue] notify failed for bookingId={}: {}", bookingId, e.getMessage());
        }
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + " - " + end.format(formatter);
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
            throw new RuntimeException("Failed to serialize QR payload", e);
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
            throw new RuntimeException("Failed to generate QR image", e);
        }
    }

    /** 3) Giải mã ngược khi cần */
    @Override
    public BookingRequest decodePayload(String base64) {
        try {
            String json = new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
            return mapper.readValue(json, BookingRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode QR payload", e);
        }
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        return mapper.convertValue(
                bookingsRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found")),
                BookingResponse.class
        );
    }
}