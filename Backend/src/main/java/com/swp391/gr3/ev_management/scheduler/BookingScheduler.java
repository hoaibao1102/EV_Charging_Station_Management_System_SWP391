package com.swp391.gr3.ev_management.scheduler;

import com.swp391.gr3.ev_management.DTO.request.ViolationRequest;
import com.swp391.gr3.ev_management.entity.Booking;
import com.swp391.gr3.ev_management.repository.BookingsRepository;
import com.swp391.gr3.ev_management.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingsRepository bookingsRepo;
    private final ViolationService violationService;

    @Scheduled(cron = "0 * * * * *")
    public void autoCancelOverdueBookings() {
        log.info("[autoCancelOverdue] Checking overdue bookings...");
        List<Booking> overdueBookings = bookingsRepo.findOverdueBookings(LocalDateTime.now());

        for (Booking b : overdueBookings) {
            try {
                Long userId = b.getVehicle().getDriver().getUser().getUserId();
                Long bookingId = b.getBookingId();

                ViolationRequest req = ViolationRequest.builder()
                        .bookingId(bookingId)
                        .description("Overdue after scheduled end time")
                        .build();

                // xác nhận giá trị trước khi gọi service
                log.info("[autoCancelOverdue] call createViolation with bookingId={}, dto.bookingId={}, userId={}",
                        bookingId, req.getBookingId(), userId);

                violationService.createViolation(userId, req);

            } catch (Exception ex) {
                log.error("[autoCancelOverdue] Error with bookingId={}", b.getBookingId(), ex);
            }
        }
    }
}
