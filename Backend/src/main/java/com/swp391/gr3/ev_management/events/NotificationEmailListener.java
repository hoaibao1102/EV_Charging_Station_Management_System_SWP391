// File: Backend/src/main/java/com/swp391/gr3/ev_management/events/NotificationEmailListener.java
package com.swp391.gr3.ev_management.events;

import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.service.BookingService;
import com.swp391.gr3.ev_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailListener {

    private final NotificationsRepository notificationsRepository;
    private final EmailService emailService;
    private final BookingService bookingService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        notificationsRepository.findById(event.notificationId()).ifPresent(n -> {
            var user = n.getUser();
            if (user == null) {
                log.debug("[NotificationEmailListener] skip: notification {} has no user", n.getNotiId());
                return;
            }
            String to = user.getEmail();
            if (to == null || to.isBlank()) {
                log.debug("[NotificationEmailListener] skip: user {} has no email", user.getUserId());
                return;
            }

            // Fallback tên hiển thị
            String displayName = (user.getName() == null || user.getName().isBlank()) ? "bạn" : user.getName();

            boolean isBookingConfirmed =
                    n.getType() == NotificationTypes.BOOKING_CONFIRMED && n.getBooking() != null;

            boolean isBookingOverdueCancelled =
                    n.getType() == NotificationTypes.BOOKING_OVERDUE && n.getBooking() != null;

            boolean isBannedNotification =
                    n.getType() == NotificationTypes.USER_BANNED;

            try {
                if (isBookingConfirmed) {
                    // Thông tin booking (null-safe)
                    var b = n.getBooking();
                    String station = (b != null && b.getStation() != null)
                            ? safe(b.getStation().getStationName()) : "trạm";
                    String timeRange = (b != null && b.getScheduledStartTime() != null && b.getScheduledEndTime() != null)
                            ? DT.format(b.getScheduledStartTime()) + " - " + DT.format(b.getScheduledEndTime())
                            : "không rõ";

                    String slotName = "N/A";
                    String connector = "";
                    if (b != null && b.getBookingSlots() != null && !b.getBookingSlots().isEmpty()) {
                        var bs = b.getBookingSlots().get(0);
                        var slot = (bs != null) ? bs.getSlot() : null;
                        slotName = "Slot " + (slot != null ? slot.getSlotId() : "N/A");
                        connector = (slot != null && slot.getConnectorType() != null)
                                ? safe(slot.getConnectorType().getDisplayName())
                                : "";
                    }

                    // Build subject riêng cho confirm (tránh "[EVMS] null")
                    String subject = "[EVMS] Xác nhận đặt chỗ #" + (b != null ? b.getBookingId() : "");

                    // Tạo QR (nếu có lỗi vẫn gửi mail bình thường)
                    byte[] qrBytes = null;
                    try {
                        if (b != null) {
                            String payload = bookingService.buildQrPayload(b.getBookingId());
                            qrBytes = bookingService.generateQrPng(payload, 320);
                        }
                    } catch (Exception qrEx) {
                        log.warn("[NotificationEmailListener] build QR failed for booking {}: {}",
                                (b != null ? b.getBookingId() : null), qrEx.getMessage());
                    }

                    // Gửi mail template confirm có QR
                    emailService.sendBookingConfirmedTpl(
                            to,
                            subject,
                            displayName,
                            (b != null ? b.getBookingId() : null),
                            station,
                            timeRange,
                            slotName,
                            connector,
                            qrBytes
                    );

                } else if (isBookingOverdueCancelled) {
                    var b = n.getBooking();
                    String station = (b != null && b.getStation() != null)
                            ? safe(b.getStation().getStationName()) : "trạm";
                    String timeRange = (b != null && b.getScheduledStartTime() != null && b.getScheduledEndTime() != null)
                            ? DT.format(b.getScheduledStartTime()) + " - " + DT.format(b.getScheduledEndTime())
                            : "không rõ";

                    String subject = "[EVMS] Booking bị hủy do quá hạn"
                            + (b != null ? (" #" + b.getBookingId()) : "");

                    emailService.sendBookingCancelledTpl(
                            to,
                            subject,
                            displayName,
                            (b != null ? b.getBookingId() : null),
                            station,
                            timeRange
                    );

                } else if (isBannedNotification) {
                    String subject = "[EVMS] Tài khoản bị khóa do vi phạm";
                    String content = "Tài khoản của bạn đã bị khóa tự động do tích lũy từ 3 vi phạm trở lên."
                            + "\nNếu bạn cho rằng đây là nhầm lẫn, vui lòng phản hồi email này hoặc liên hệ bộ phận hỗ trợ.";

                    emailService.sendNotificationEmailTpl(
                            to,
                            subject,
                            displayName,
                            "Tài khoản bị khóa do vi phạm",
                            content,
                            n.getType(),
                            n.getStatus(),
                            n.getCreatedAt()
                    );

                } else {
                    // Mặc định: dùng template chung
                    String fallbackSubject = "[EVMS] " + (n.getTitle() != null ? n.getTitle() : "Thông báo");
                    emailService.sendNotificationEmailTpl(
                            to,
                            fallbackSubject,
                            displayName,
                            safe(n.getTitle()),
                            safe(n.getContentNoti()),
                            n.getType(),
                            n.getStatus(),
                            n.getCreatedAt()
                    );
                }
            } catch (Exception mailEx) {
                // Không để exception này làm fail thread listener
                log.error("[NotificationEmailListener] failed to send email for notification {}: {}",
                        n.getNotiId(), mailEx.getMessage(), mailEx);
            }
        });
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
