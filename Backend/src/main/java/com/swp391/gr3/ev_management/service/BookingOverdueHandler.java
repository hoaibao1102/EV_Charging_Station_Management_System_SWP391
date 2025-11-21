package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ViolationRequest;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service // Đánh dấu đây là một Spring Service (bean xử lý logic nghiệp vụ)
@RequiredArgsConstructor // Tự động sinh constructor với các field final
@Slf4j // Cho phép sử dụng logger (log.info, log.error,...)
public class BookingOverdueHandler {

    // Múi giờ cố định cho hệ thống (theo giờ Việt Nam)
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingsRepository bookingsRepository;           // Repository để thao tác bảng Booking
    private final NotificationsService notificationsService;       // Service để lưu Notification
    private final ApplicationEventPublisher eventPublisher;        // Dùng để bắn Event trong Spring (publish event)
    private final ViolationService violationService;               // Service xử lý vi phạm (tạo Violation)

    /**
     * Phương thức này được gọi khi 1 booking bị quá hạn (overdue).
     * Nó sẽ:
     *  1) Hủy booking (nếu đang ở trạng thái CONFIRMED)
     *  2) Tạo bản ghi Violation cho tài xế
     *  3) Gửi thông báo (Notification) cho người dùng
     *
     * Được bọc trong một transaction mới (Propagation.REQUIRES_NEW)
     * để đảm bảo độc lập với các transaction khác (ngăn rollback dây chuyền).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAndCreateViolationTx(Long bookingId) {
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // ✅ 1) Update status nếu đang CONFIRMED hoặc PENDING
        int rows = bookingsRepository.updateStatusIfIn(
                bookingId,
                BookingStatus.CANCELED,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING),
                now
        );

        if (rows == 0) {
            var cur = bookingsRepository.findStatusOnly(bookingId);
            log.debug("[overdue] skip cancel bookingId={} (currentStatus={})",
                    bookingId, cur.orElse(null));
            return;
        }

        log.info("[overdue] CANCELED bookingId={} at {}", bookingId, now);

        // ✅ 2) Lấy dữ liệu nhẹ để không JOIN nặng
        var optView = bookingsRepository.findOverdueView(bookingId);
        if (optView.isEmpty()) {
            log.warn("[overdue] BookingOverdueView not found for bookingId={}", bookingId);
            return;
        }
        var view = optView.get();

        Long userId = view.getUserId();
        String stationName = view.getStationName();
        LocalDateTime endTime = view.getScheduledEndTime();

        // ===================== TẠO VIOLATION =====================
        try {
            ViolationRequest vr = ViolationRequest.builder()
                    .bookingId(bookingId)
                    .description(String.format(
                            "Booking #%d đã quá hạn và bị hủy tự động (đến %s tại trạm %s).",
                            bookingId, endTime, stationName))
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

        // ===================== GỬI NOTIFICATION =====================
        try {
            Notification noti = new Notification();

            // ⚠️ Không load Booking/User nặng nữa — chỉ gắn "stub" có ID
            Booking bookingRef = new Booking();
            bookingRef.setBookingId(bookingId);

            User userRef = new User();
            userRef.setUserId(userId);

            noti.setBooking(bookingRef);
            noti.setUser(userRef);
            noti.setTitle("Booking bị hủy do quá hạn");
            noti.setContentNoti("Booking #" + bookingId + " tại trạm " + stationName + " đã bị hủy vì quá giờ.");
            noti.setType(NotificationTypes.BOOKING_OVERDUE);
            noti.setStatus(Notification.STATUS_UNREAD);

            notificationsService.save(noti);

            try {
                eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
            } catch (Exception mailEx) {
                log.warn("[overdue] publish email event failed bookingId={}: {}",
                        bookingId, mailEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("[overdue] notify failed bookingId={}: {}", bookingId, e.getMessage());
        }
    }
}
