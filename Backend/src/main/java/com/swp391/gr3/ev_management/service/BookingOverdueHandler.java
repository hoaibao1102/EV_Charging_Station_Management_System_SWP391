package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ViolationRequest;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOverdueHandler {

    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingsRepository bookingsRepository;
    private final NotificationsRepository notificationsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ViolationService violationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAndCreateViolationTx(Long bookingId) {
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // Atomic update để chống race-condition
        int rows = bookingsRepository.updateStatusIfMatches(
                bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELED, now);
        if (rows == 0) {
            var cur = bookingsRepository.findStatusOnly(bookingId);
            log.debug("[overdue] skip cancel bookingId={} (currentStatus={})", bookingId, cur.orElse(null));
            return;
        }
        log.info("[overdue] CANCELED bookingId={} at {}", bookingId, now);

        Booking booking = bookingsRepository.findByIdWithAllNeeded(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found after cancel: " + bookingId));

        // Violation (bọc lỗi để không rollback)
        try {
            Long userId = booking.getVehicle().getDriver().getUser().getUserId();
            ViolationRequest vr = ViolationRequest.builder()
                    .bookingId(bookingId)
                    .description(String.format(
                            "Booking #%d đã quá hạn và bị hủy tự động (đến %s tại trạm %s).",
                            bookingId, booking.getScheduledEndTime(), booking.getStation().getStationName()))
                    .build();

            var resp = violationService.createViolation(userId, vr);
            if (resp == null) {
                log.info("[overdue] violation NOT created (resp=null) bookingId={}", bookingId);
            } else {
                log.info("[overdue] violation created bookingId={} violationId={}",
                        bookingId, resp.getViolationId());
            }
        } catch (Exception e) {
            log.error("[overdue] createViolation failed bookingId={}", bookingId, e);
        }

        // Notification (bọc lỗi)
        try {
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser());
            noti.setTitle("Booking bị hủy do quá hạn");
            noti.setContentNoti("Booking #" + bookingId + " đã bị hủy vì quá giờ.");
            noti.setType(NotificationTypes.BOOKING_OVERDUE);
            noti.setStatus(Notification.STATUS_UNREAD);
            noti.setBooking(booking);
            notificationsRepository.save(noti);

            try {
                eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
            } catch (Exception mailEx) {
                log.warn("[overdue] publish email event failed bookingId={}: {}", bookingId, mailEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("[overdue] notify failed bookingId={}: {}", bookingId, e.getMessage());
        }
    }
}
