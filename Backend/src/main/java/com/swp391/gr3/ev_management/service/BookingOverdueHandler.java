package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ViolationRequest;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.enums.NotificationTypes;
import com.swp391.gr3.ev_management.events.NotificationCreatedEvent;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
        // Lấy thời gian hiện tại theo múi giờ Việt Nam
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);

        // Thực hiện update trạng thái booking thành CANCELED
        // Chỉ update nếu booking hiện đang có trạng thái CONFIRMED
        // -> tránh race condition khi nhiều tiến trình cùng chạy
        int rows = bookingsRepository.updateStatusIfMatches(
                bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELED, now);

        // Nếu không có hàng nào bị ảnh hưởng (nghĩa là không thỏa điều kiện update)
        if (rows == 0) {
            // Ghi log trạng thái hiện tại và bỏ qua
            var cur = bookingsRepository.findStatusOnly(bookingId);
            log.debug("[overdue] skip cancel bookingId={} (currentStatus={})", bookingId, cur.orElse(null));
            return; // Dừng xử lý
        }

        // Ghi log thành công việc hủy booking
        log.info("[overdue] CANCELED bookingId={} at {}", bookingId, now);

        // Lấy lại booking đầy đủ thông tin (để dùng cho các bước tiếp theo)
        Booking booking = bookingsRepository.findByIdWithAllNeeded(bookingId)
                .orElseThrow(() -> new ErrorException("Booking not found after cancel: " + bookingId));

        // ===========================================
        // BẮT ĐẦU TẠO VIOLATION (bọc trong try-catch để tránh rollback toàn bộ)
        // ===========================================
        try {
            // Lấy userId của tài xế có liên quan đến booking
            Long userId = booking.getVehicle().getDriver().getUser().getUserId();

            // Tạo request cho ViolationService
            ViolationRequest vr = ViolationRequest.builder()
                    .bookingId(bookingId)
                    .description(String.format(
                            "Booking #%d đã quá hạn và bị hủy tự động (đến %s tại trạm %s).",
                            bookingId, booking.getScheduledEndTime(), booking.getStation().getStationName()))
                    .build();

            // Gọi service để tạo Violation
            var resp = violationService.createViolation(userId, vr);

            // Ghi log kết quả tạo Violation
            if (resp == null) {
                log.info("[overdue] violation NOT created (resp=null) bookingId={}", bookingId);
            } else {
                log.info("[overdue] violation created bookingId={} violationId={}",
                        bookingId, resp.getViolationId());
            }
        } catch (Exception e) {
            // Nếu tạo violation thất bại, ghi log lỗi nhưng không rollback transaction
            log.error("[overdue] createViolation failed bookingId={}", bookingId, e);
        }

        // ===========================================
        // GỬI THÔNG BÁO (Notification)
        // ===========================================
        try {
            // Tạo một notification mới cho người dùng
            Notification noti = new Notification();
            noti.setUser(booking.getVehicle().getDriver().getUser()); // Gửi cho tài xế
            noti.setTitle("Booking bị hủy do quá hạn");               // Tiêu đề
            noti.setContentNoti("Booking #" + bookingId + " đã bị hủy vì quá giờ."); // Nội dung
            noti.setType(NotificationTypes.BOOKING_OVERDUE);          // Loại thông báo
            noti.setStatus(Notification.STATUS_UNREAD);               // Đặt trạng thái là chưa đọc
            noti.setBooking(booking);                                 // Gắn booking liên quan

            // Lưu notification vào DB
            notificationsService.save(noti);

            // Sau khi lưu, bắn sự kiện để các listener khác (ví dụ gửi email) xử lý
            try {
                eventPublisher.publishEvent(new NotificationCreatedEvent(noti.getNotiId()));
            } catch (Exception mailEx) {
                // Nếu lỗi trong việc publish event (như gửi mail thất bại) -> chỉ log warning
                log.warn("[overdue] publish email event failed bookingId={}: {}", bookingId, mailEx.getMessage());
            }
        } catch (Exception e) {
            // Nếu lỗi khi lưu hoặc gửi thông báo -> ghi log warning
            log.warn("[overdue] notify failed bookingId={}: {}", bookingId, e.getMessage());
        }
    }
}
