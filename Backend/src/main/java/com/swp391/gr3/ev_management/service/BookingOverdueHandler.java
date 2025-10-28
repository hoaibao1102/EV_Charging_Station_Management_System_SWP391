package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOverdueHandler {
    private final BookingsRepository bookingsRepository;
    private final NotificationsRepository notificationsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ViolationService violationService;

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
}
