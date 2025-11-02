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

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private static final ZoneId TENANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingsRepository bookingsRepo;
    private final BookingOverdueHandler overdueHandler;

    // Mỗi phút theo giờ VN
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoCancelOverdueBookings() {
        LocalDateTime now = LocalDateTime.now(TENANT_ZONE);
        log.info("[autoCancelOverdue] tick at {}", now);

        List<Booking> overdue = bookingsRepo
                .findTop50ByStatusAndScheduledEndTimeLessThanEqualOrderByScheduledEndTimeAsc(
                        BookingStatus.CONFIRMED, now);

        log.info("[autoCancelOverdue] found {} bookings overdue", overdue.size());

        for (Booking b : overdue) {
            try {
                overdueHandler.cancelAndCreateViolationTx(b.getBookingId());
            } catch (Exception ex) {
                log.error("[autoCancelOverdue] Error bookingId={}: {}", b.getBookingId(), ex.getMessage(), ex);
            }
        }
    }
}
