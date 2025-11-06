package com.swp391.gr3.ev_management.events;

import com.swp391.gr3.ev_management.entity.Invoice;
import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.repository.NotificationsRepository;
import com.swp391.gr3.ev_management.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationEmailListener {

    private final NotificationsRepository notificationsRepository;
    private final EmailService emailService;

    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        notificationsRepository.findById(event.notificationId()).ifPresent(n -> {
            var type = n.getType();
            if (type != NotificationTypes.PAYMENT_SUCCESS && type != NotificationTypes.PAYMENT_FAILED) {
                return; // chỉ xử lý payment
            }

            var user = n.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.debug("[PaymentNotificationEmailListener] skip: notification {} has no user/email", n.getNotiId());
                return;
            }

            final String to = user.getEmail();
            final String displayName = (user.getName() == null || user.getName().isBlank()) ? "bạn" : user.getName();

            // Lấy transaction từ notification
            Transaction tx = n.getTransaction();
            if (tx == null) {
                log.debug("[PaymentNotificationEmailListener] skip: notification {} has no transaction", n.getNotiId());
                return;
            }

            // Suy ra invoice & session qua transaction
            Invoice invoice = tx.getInvoice();
            var session  = (invoice != null) ? invoice.getSession() : n.getSession();
            var booking  = (session != null) ? session.getBooking() : null;
            var station  = (booking != null) ? booking.getStation() : null;

            String stationName = (station != null) ? safe(station.getStationName()) : "trạm sạc";
            String timeRange =
                    (session != null && session.getStartTime() != null && session.getEndTime() != null)
                            ? DT.format(session.getStartTime()) + " - " + DT.format(session.getEndTime())
                            : (booking != null && booking.getScheduledStartTime() != null && booking.getScheduledEndTime() != null)
                            ? DT.format(booking.getScheduledStartTime()) + " - " + DT.format(booking.getScheduledEndTime())
                            : "";

            String amountStr = (tx.getAmount() > 0)
                    ? fmtAmount(tx.getAmount(), tx.getCurrency())
                    : "";

            Long invoiceId = (invoice != null) ? invoice.getInvoiceId() : null;

            try {
                String subject;
                String title;
                String content;

                if (type == NotificationTypes.PAYMENT_SUCCESS) {
                    subject = "[EVMS] Thanh toán thành công" + (invoiceId != null ? " #" + invoiceId : "");
                    title   = "Thanh toán thành công hóa đơn" + (invoiceId != null ? (" #" + invoiceId) : "");
                    content = "Số tiền: " + amountStr
                            + " | Trạm: " + stationName
                            + (timeRange.isEmpty() ? "" : " | Thời gian: " + timeRange);
                } else { // PAYMENT_FAILED
                    subject = "[EVMS] Thanh toán thất bại" + (invoiceId != null ? " #" + invoiceId : "");
                    title   = "Thanh toán thất bại" + (invoiceId != null ? (" cho hóa đơn #" + invoiceId) : "");
                    content = "Số tiền: " + amountStr
                            + " | Trạm: " + stationName
                            + ". Vui lòng thử lại hoặc chọn phương thức khác.";
                }

                // Gửi bằng template chung
                emailService.sendNotificationEmailTpl(
                        to,
                        subject,
                        displayName,
                        title,
                        content,
                        n.getType(),
                        n.getStatus(),
                        n.getCreatedAt()
                );

            } catch (Exception mailEx) {
                log.error("[PaymentNotificationEmailListener] failed to send email for notification {}: {}",
                        n.getNotiId(), mailEx.getMessage(), mailEx);
            }
        });
    }

    private static String fmtAmount(double amount, String currency) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " " + (currency != null ? currency : "VND");
    }
    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}
