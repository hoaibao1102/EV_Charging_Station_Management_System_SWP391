package com.swp391.gr3.ev_management.events;

import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.service.BookingService;
import com.swp391.gr3.ev_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEmailListener {

    private final NotificationsRepository notificationsRepository;
    private final EmailService emailService;
    private final BookingService bookingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        notificationsRepository.findById(event.notificationId()).ifPresent(n -> {
            var user = n.getUser();
            if (user == null) return;
            String to = user.getEmail();
            if (to == null || to.isBlank()) return;

            String subject = "[EVMS] " + safe(n.getTitle());
            String displayName = (user.getName() == null || user.getName().isBlank()) ? "bạn" : user.getName();

            boolean isBookingConfirmed =
                    n.getType() == NotificationTypes.BOOKING_CONFIRMED && n.getBooking() != null;

            if (isBookingConfirmed) {
                // ----- Email có QR (Template: booking-confirmed.html) -----
                byte[] qrBytes = null;
                try {
                    Long bookingId = n.getBooking().getBookingId();
                    String payload = bookingService.buildQrPayload(bookingId);
                    qrBytes = bookingService.generateQrPng(payload, 320);
                } catch (Exception ignored) {}

                // html bị bỏ qua vì EmailService sẽ tự render Thymeleaf bên trong
                emailService.sendHtmlWithInline(
                        to,
                        subject,
                        null,      // htmlIgnored
                        "qr",      // CID trùng với th:src="'cid:' + ${cid}"
                        qrBytes
                );
            } else {
                // ----- Email notification chung (Template: email-notification.html) -----
                emailService.sendNotificationEmailTpl(
                        to,
                        subject,
                        displayName,
                        n.getTitle(),
                        n.getContentNoti(),
                        n.getType(),
                        n.getStatus(),
                        n.getCreatedAt()
                );
            }
        });
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
