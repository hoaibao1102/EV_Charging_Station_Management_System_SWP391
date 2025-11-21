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
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);
        log.info("[autoCancelOverdue] tick at {}", now);

        // ✅ chỉ lấy ID, không JOIN gì nặng cả
        List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);

        List<Long> overdueIds = bookingsRepo.findOverdueIds(statuses, now);
        log.info("[autoCancelOverdue] found {} bookings overdue", overdueIds.size());

        for (Long bookingId : overdueIds) {
            try {
                overdueHandler.cancelAndCreateViolationTx(bookingId);
            } catch (Exception ex) {
                log.error("[autoCancelOverdue] Error bookingId={}: {}",
                        bookingId, ex.getMessage(), ex);
            }
        }
    }
}
