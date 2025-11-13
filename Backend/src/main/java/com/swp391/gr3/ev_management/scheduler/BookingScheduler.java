package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.enums.BookingStatus;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.service.BookingOverdueHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component                           // Đánh dấu class này là Spring Bean dùng cho Scheduler
@RequiredArgsConstructor             // Lombok tạo constructor tự động cho các field final
@Slf4j                               // Sinh ra logger cho class
public class BookingScheduler {

    // Múi giờ Việt Nam (để đảm bảo xử lý đúng thời gian theo địa phương)
    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingsRepository bookingsRepo;      // Repository để truy vấn các booking quá hạn
    private final BookingOverdueHandler overdueHandler; // Service xử lý hủy + tạo violation

    // Chạy mỗi phút (giây 0 của mỗi phút) theo giờ Việt Nam
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoCancelOverdueBookings() {
        // Lấy thời gian hiện tại theo múi giờ VN
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);
        log.info("[autoCancelOverdue] tick at {}", now);

        // Lấy tối đa 50 booking đang CONFIRMED mà scheduledEndTime <= now
        // => nghĩa là đã quá hạn kết thúc nhưng user chưa đến => cần auto-cancel
        List<Booking> overdue = bookingsRepo
                .findTop50ByStatusAndScheduledEndTimeLessThanEqualOrderByScheduledEndTimeAsc(
                        BookingStatus.CONFIRMED, now);

        log.info("[autoCancelOverdue] found {} bookings overdue", overdue.size());

        // Lặp qua từng booking quá hạn để xử lý
        for (Booking b : overdue) {
            try {
                // Gọi handler xử lý: bao gồm cancel booking + tạo violation (nếu phù hợp)
                overdueHandler.cancelAndCreateViolationTx(b.getBookingId());
            } catch (Exception ex) {
                // Nếu có lỗi, log chi tiết + bookingId để dễ trace
                log.error("[autoCancelOverdue] Error bookingId={}: {}", b.getBookingId(), ex.getMessage(), ex);
            }
        }
    }
}
